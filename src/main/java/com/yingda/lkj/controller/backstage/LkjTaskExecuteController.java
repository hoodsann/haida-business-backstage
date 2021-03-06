package com.yingda.lkj.controller.backstage;

import com.yingda.lkj.beans.entity.backstage.dataversion.DataApproveFlow;
import com.yingda.lkj.beans.entity.backstage.lkj.LkjDataLine;
import com.yingda.lkj.beans.entity.backstage.lkj.LkjTask;
import com.yingda.lkj.beans.entity.backstage.organization.Organization;
import com.yingda.lkj.beans.entity.system.User;
import com.yingda.lkj.beans.enums.dataversion.ApproveDataType;
import com.yingda.lkj.beans.exception.CustomException;
import com.yingda.lkj.beans.pojo.lkj.LkjDataLineFromExcel;
import com.yingda.lkj.beans.pojo.device.Semaphore;
import com.yingda.lkj.beans.pojo.utils.ExcelSheetInfo;
import com.yingda.lkj.beans.system.Json;
import com.yingda.lkj.beans.system.JsonMessage;
import com.yingda.lkj.controller.BaseController;
import com.yingda.lkj.service.backstage.dataapprove.DataApproveConfigService;
import com.yingda.lkj.service.backstage.dataapprove.DataApproveFlowService;
import com.yingda.lkj.service.backstage.lkjdataline.LkjDataLineService;
import com.yingda.lkj.service.backstage.lkjtask.LkjTaskService;
import com.yingda.lkj.service.backstage.organization.OrganizationClientService;
import com.yingda.lkj.service.base.BaseService;
import com.yingda.lkj.utils.RequestUtil;
import com.yingda.lkj.utils.StringUtils;
import com.yingda.lkj.utils.excel.ExcelUtil;
import com.yingda.lkj.utils.excel.excelClient.LkjDataExcelParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author hood  2020/3/1
 */
@Controller
@RequestMapping("/backstage/lkjTaskExecute")
public class LkjTaskExecuteController extends BaseController {

    @Autowired
    private BaseService<LkjTask> lkjTaskBaseService;
    @Autowired
    private OrganizationClientService organizationClientService;
    @Autowired
    private LkjDataLineService lkjDataLineService;
    @Autowired
    private LkjTaskService lkjTaskService;
    @Autowired
    private BaseService<LkjDataLine> lkjDataLineBaseService;
    @Autowired
    private DataApproveConfigService dataApproveConfigService;
    @Autowired
    private DataApproveFlowService dataApproveFlowService;

    private DataApproveFlow pageDataApproveFlow;

    @RequestMapping("")
    public ModelAndView getList() throws Exception {
        User user = RequestUtil.getUser(req);

        String taskName = req.getParameter("taskName");
        String finishedStatus = req.getParameter("finishedStatus");
        Map<String, Object> attribute = new HashMap<>();

        String hql = "from LkjTask where executeUserId = :executeUserId order by addTime desc";
        Map<String, Object> params = new HashMap<>();
        params.put("executeUserId", user.getId());
        List<LkjTask> lkjTasks = lkjTaskBaseService.find(hql, params, page.getCurrentPage(), page.getPageSize());

        if (StringUtils.isNotEmpty(taskName)){
            lkjTasks = lkjTasks.stream().filter(x -> x.getName().contains(taskName)).collect(Collectors.toList());
            attribute.put("taskName", taskName);
        }
        if (StringUtils.isNotEmpty(finishedStatus)){
            lkjTasks = lkjTasks.stream().filter(x -> Byte.parseByte(finishedStatus) == x.getFinishedStatus()).collect(Collectors.toList());
            params.put("finishedStatus", Byte.parseByte(finishedStatus));
            attribute.put("finishedStatus", finishedStatus);
        }
        Long count = lkjTaskBaseService.getObjectNum(LkjTask.class, params, Map.of("executeUserId", "=", "finishedStatus", "="));
        page.setDataTotal(count);
        attribute.put("lkjTasks", lkjTasks);
        attribute.put("page", page);
        return new ModelAndView("/backstage/lkjtaskexecute/lkj-task-execute-list", attribute);
    }

    /**
     * ??????????????????
     */
    @RequestMapping("/{id}")
    public ModelAndView taskDetail(@PathVariable String id) throws Exception {
        List<LkjDataLine> lkjDataLines = lkjDataLineBaseService.find("from LkjDataLine where lkjTaskId = :lkjTaskId order by seq, uniqueCode, " +
                "addTime desc", Map.of("lkjTaskId", id));
        List<Semaphore> semaphores = lkjDataLineService.expandLkjDataLine(lkjDataLines);

        LkjTask lkjTask = lkjTaskBaseService.get(LkjTask.class, id);

        return new ModelAndView("/backstage/lkjtaskexecute/lkj-task-execute-detail", Map.of("semaphores", semaphores, "lkjTask", lkjTask));
    }

    /**
     * ??????????????????
     */
    @RequestMapping("/fillTask")
    @ResponseBody
    public Json fillTask(MultipartFile file) throws Exception {
        String lkjTaskId = req.getParameter("lkjTaskId");
        LkjTask lkjTask = lkjTaskBaseService.get(LkjTask.class, lkjTaskId);

        String sectionId = lkjTask.getSectionId();
        String fragmentId = lkjTask.getFragmentId();
        Organization bureau = organizationClientService.getParent(sectionId);

        List<ExcelSheetInfo> excelSheetInfos = ExcelUtil.readExcelFile(file);
        List<LkjDataLineFromExcel> lkjDataLineFromExcel = new LkjDataExcelParser(bureau.getCode()).getLkjDataLineFromExcel(excelSheetInfos);
        List<LkjDataLine> rawLkjDataLines = lkjDataLineService.wrapLkjDataLine(lkjDataLineFromExcel, fragmentId);

        lkjDataLineService.fillTask(lkjTaskId, rawLkjDataLines);

        return new Json(JsonMessage.SUCCESS);
    }

    @RequestMapping("/updateLkjDataLine")
    @ResponseBody
    public Json updateLkjDataLine() throws Exception {
        String lkjDataLineId = req.getParameter("lkjDataLineId");
        String distanceStr = req.getParameter("distance");


        LkjDataLine lkjDataLine = lkjDataLineBaseService.get(LkjDataLine.class, lkjDataLineId);

        if (lkjDataLine == null)
            throw new CustomException(new Json(JsonMessage.SYS_ERROR, "????????? id = " + lkjDataLineId + " ???lkj???????????????????????????"));

        String lkjTaskId = lkjDataLine.getLkjTaskId();
        if (StringUtils.isEmpty(lkjTaskId))
            throw new CustomException(new Json(JsonMessage.SYS_ERROR, "????????? id = " + lkjDataLineId + " ????????????????????????????????????"));

        LkjTask lkjTask = lkjTaskBaseService.get(LkjTask.class, lkjTaskId);
        if (lkjTask.getFinishedStatus() != LkjTask.PENDING_HANDLE)
            throw new CustomException(new Json(JsonMessage.SYS_ERROR, "??????????????????????????????")); // ??????????????????????????????????????????

        double distance = Double.parseDouble(distanceStr);
        lkjDataLine.setDistance(distance);
        lkjDataLineBaseService.saveOrUpdate(lkjDataLine);

        return new Json(JsonMessage.SUCCESS, "????????????");
    }

    /**
     * ???????????????????????????
     */
    @RequestMapping("/importPage")
    public ModelAndView importPage(String lkjTaskId) throws Exception {
        User user = RequestUtil.getUser(req);

        LkjTask lkjTask = lkjTaskBaseService.get(LkjTask.class, lkjTaskId);

        List<User> availableApproveUsers = dataApproveConfigService.getAvailableApproveUsers(user);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("lkjTask", lkjTask);
        attributes.put("availableApproveUsers", availableApproveUsers);
        return new ModelAndView("/backstage/lkjtaskexecute/lkj-task-execute-import", attributes);
    }

    /**
     * ??????lkj?????????????????????lkj???????????????
     */
    @ResponseBody
    @RequestMapping("/executeLkjTask")
    public Json executeLkjTask() throws Exception {
        String approveUserId = req.getParameter("approveUserId");
        String lkjTaskId = req.getParameter("lkjTaskId");


        User user = RequestUtil.getUser(req);

        List<User> availableApproveUsers = dataApproveConfigService.getAvailableApproveUsers(user);
        if (StringUtils.isEmpty(approveUserId) && !availableApproveUsers.isEmpty())
            return new Json(JsonMessage.PARAM_INVALID, "??????????????????????????????????????????????????????");

        if (StringUtils.isNotEmpty(approveUserId)
                && availableApproveUsers.stream().noneMatch(x -> x.getId().equals(approveUserId)))
            return new Json(JsonMessage.PARAM_INVALID, "???????????????????????????????????????????????????????????????");


        DataApproveFlow dataApproveFlow = lkjTaskService.submitLkjTask(lkjTaskId, pageDataApproveFlow, user, approveUserId);

        if (StringUtils.isEmpty(approveUserId))
            dataApproveFlowService.completeDataApproveFlow(dataApproveFlow.getId(), ApproveDataType.LKJ14);

        return new Json(JsonMessage.SUCCESS);
    }

    @ModelAttribute
    public void setPageDataApproveFlow(DataApproveFlow pageDataApproveFlow) {
        this.pageDataApproveFlow = pageDataApproveFlow;
    }
}
