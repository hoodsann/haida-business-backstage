package com.yingda.lkj.controller.system;

import com.yingda.lkj.beans.entity.backstage.device.Device;
import com.yingda.lkj.beans.entity.backstage.measurement.MeasurementTaskDetail;
import com.yingda.lkj.beans.entity.system.Menu;
import com.yingda.lkj.beans.entity.system.User;
import com.yingda.lkj.beans.exception.CustomException;
import com.yingda.lkj.beans.pojo.device.DeviceStatistics;
import com.yingda.lkj.beans.pojo.lkj.lkjtask.UserLkjTask;
import com.yingda.lkj.beans.pojo.measurement.MeasurementTaskStatistics;
import com.yingda.lkj.beans.pojo.measurement.UserMeasurementTaskDetail;
import com.yingda.lkj.beans.system.Json;
import com.yingda.lkj.beans.system.JsonMessage;
import com.yingda.lkj.controller.BaseController;
import com.yingda.lkj.service.backstage.device.DeviceService;
import com.yingda.lkj.service.backstage.line.StationRailwayLineService;
import com.yingda.lkj.service.backstage.lkjtask.LkjTaskService;
import com.yingda.lkj.service.backstage.measurement.MeasurementTaskService;
import com.yingda.lkj.service.base.BaseService;
import com.yingda.lkj.service.system.AuthService;
import com.yingda.lkj.service.system.MenuService;
import com.yingda.lkj.utils.RequestUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import java.util.*;

/**
 * @author hood  2019/12/18
 */
@Controller
public class IndexController extends BaseController {

    @Autowired
    private AuthService authService;
    @Autowired
    private MenuService menuService;
    @Autowired
    private BaseService<Menu> menuBaseService;
    @Autowired
    private LkjTaskService lkjTaskService;
    @Autowired
    private DeviceService deviceService;
    @Autowired
    private BaseService<MeasurementTaskDetail> measurementTaskDetailBaseService;
    @Autowired
    private MeasurementTaskService measurementTaskService;

    @RequestMapping("/")
    public ModelAndView login() throws CustomException {
        return index();
    }

    @RequestMapping("/index")
    public ModelAndView index() throws CustomException {
        Map<String, Object> attributes = new HashMap<>();

        User user = getUser();
        List<Menu> valuableMenus = authService.getValuableMenus(user);
        // ????????????
        List<Menu> availableMenus = menuService.jsonified(valuableMenus);
        // ?????????
        List<Menu> slaves = menuService.getSlave(req.getParameter("parentId"), valuableMenus);

        attributes.put("menus", availableMenus);
        attributes.put("slaves", slaves);
        attributes.put("user", user);

        return new ModelAndView("index", attributes);
    }

    @RequestMapping("/welcome")
    public ModelAndView welcome() {
        User user = RequestUtil.getUser(req);
        String userId = user.getId();
        UserLkjTask userLkjTask = lkjTaskService.userTaskInfo(userId);
        UserMeasurementTaskDetail userMeasurementTaskDetail = measurementTaskService.getUserMeasurementTaskDetail(userId);

        return new ModelAndView(
                "/welcome",
                Map.of("user", user, "userLkjTask", userLkjTask, "userMeasurementTaskDetail", userMeasurementTaskDetail)
        );
    }

    @RequestMapping("/indexCharts")
    @ResponseBody
    public Json indexCharts() throws Exception {
//        String startTimeStr = req.getParameter("startTimeStr");
//        String endTimeStr = req.getParameter("endTimeStr");

        List<Device> devices = deviceService.getBySectionId(getSectionId());
        DeviceStatistics deviceStatistics = new DeviceStatistics(devices);

//        List<MeasurementTaskDetail> measurementTaskDetails = measurementTaskDetailBaseService.find(
//                "from MeasurementTaskDetail where addTime > :startTime and addTime < :endTime",
//                Map.of("startTime", DateUtil.toTimestamp(startTimeStr), "endTime", DateUtil.toTimestamp(endTimeStr))
//        );
        List<MeasurementTaskDetail> measurementTaskDetails = measurementTaskDetailBaseService.find(
                "from MeasurementTaskDetail"
        );
        MeasurementTaskStatistics measurementTaskStatistics = new MeasurementTaskStatistics(measurementTaskDetails);

        return new Json(JsonMessage.SUCCESS, Map.of("deviceStatistics", deviceStatistics, "measurementTaskStatistics", measurementTaskStatistics));
    }

    private String mainMenus = "??????,????????????,????????????,????????????,????????????,???????????????,????????????,????????????,????????????";
    private String sec = "?????????,\n" +
            "????????????,????????????,????????????,??????,??????/?????????,?????????,?????????,??????,??????,?????????,????????????,??????,\n" +
            "????????????,??????,????????????,?????????,??????????????????,\n" +
            "??????,??????,\n" +
            "????????????,\n" +
            "???????????????,????????????,????????????,????????????,\n" +
            "????????????,????????????,\n" +
            "?????????,?????????,??????,??????,?????????,k??????,?????????,?????????,\n" +
            "????????????,??????,????????????,?????????,??????????????????";

    private String secUrl = "/html/welcome,\n" +
            "/html/unicode,html/form1,html/form2,html/buttons,html/nav,html/tab,html/progressBar,html/panel,html/badge,html/timeline,html/tableElement," +
            "html/anim,\n" +
            "html/upload,html/page,html/cate,html/carousel,html/city,\n" +
            "html/grid,html/welcome2,\n" +
            "html/orderList,\n" +
            "html/adminList,/role,html/adminCate,/menu,\n" +
            "html/memberList,html/memberDel,\n" +
            "html/echarts1,html/echarts2,html/echarts3,html/echarts4,html/echarts5,html/echarts6,html/echarts7,html/echarts8,\n" +
            "html/upload,html/page,html/cate,html/carousel,html/city";

    @RequestMapping("test111")
    @ResponseBody
    public Json testaaaa() {
        List<Menu> akagi = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            String mainId = UUID.randomUUID().toString();
            String mainName = mainMenus.split(",")[i];
            Menu menu = new Menu(mainId, Menu.ROOT_ID, "-1", mainName, i, Menu.PRIMARY_MENU);
            akagi.add(menu);

            String[] didiNames = sec.split("\\n")[i].split(",");
            String[] didiUrls = secUrl.split("\\n")[i].split(",");

            for (int j = 0; j < didiNames.length; j++) {
                String didiName = didiNames[j];
                String didiUrl = didiUrls[j];
                Menu menu1 = new Menu(UUID.randomUUID().toString(), menu.getId(), didiUrl, didiName, j, Menu.SECONDARY_MENU);
                akagi.add(menu1);
            }
        }

        menuBaseService.bulkInsert(akagi);

        return new Json(JsonMessage.SUCCESS);
    }

}
