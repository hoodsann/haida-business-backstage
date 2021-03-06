package com.yingda.lkj.controller.backstage;

import com.yingda.lkj.beans.entity.backstage.device.Device;
import com.yingda.lkj.beans.entity.backstage.measurement.*;
import com.yingda.lkj.beans.exception.CustomException;
import com.yingda.lkj.beans.pojo.measurement.MeasurementItemFieldValueChatsPojo;
import com.yingda.lkj.beans.pojo.utils.ExcelRowInfo;
import com.yingda.lkj.beans.pojo.utils.ExcelSheetInfo;
import com.yingda.lkj.beans.system.Json;
import com.yingda.lkj.beans.system.JsonMessage;
import com.yingda.lkj.controller.BaseController;
import com.yingda.lkj.service.backstage.device.DeviceService;
import com.yingda.lkj.service.backstage.device.DeviceSubTypeService;
import com.yingda.lkj.service.backstage.device.DeviceTypeService;
import com.yingda.lkj.service.backstage.line.StationService;
import com.yingda.lkj.service.backstage.measurement.MeasurementItemFieldValueService;
import com.yingda.lkj.service.backstage.measurement.MeasurementTaskDetailService;
import com.yingda.lkj.service.backstage.measurement.MeasurementTemplateService;
import com.yingda.lkj.service.base.BaseService;
import com.yingda.lkj.utils.StreamUtil;
import com.yingda.lkj.utils.StringUtils;
import com.yingda.lkj.utils.date.DateUtil;
import com.yingda.lkj.utils.excel.ExcelUtil;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author hood  2020/6/25
 */
@Controller
@RequestMapping("/backstage/measurementHistoryChats")
public class MeasurementHistoryCharts extends BaseController {

    @Autowired
    private MeasurementTemplateService measurementTemplateService;
    @Autowired
    private BaseService<MeasurementItemFieldValue> measurementItemFieldValueBaseService;
    @Autowired
    private MeasurementTaskDetailService measurementTaskDetailService;
    @Autowired
    private BaseService<MeasurementTaskDetail> measurementTaskDetailBaseService;
    @Autowired
    private MeasurementItemFieldValueService measurementItemFieldValueService;
    @Autowired
    private DeviceService deviceService;

    /**
     * ???????????????????????????
     */
    @RequestMapping("/charts")
    public ModelAndView detailCharts() {
        Map<String, Object> attributes = new HashMap<>();

        String deviceId = req.getParameter("deviceId");
        String measurementTemplateId = req.getParameter("measurementTemplateId");

        // ??????????????????
        List<MeasurementItem> measurementItems = measurementTemplateService.getItemsAndItemFieldsByTemplateId(measurementTemplateId);
        // ???????????????????????????????????????????????????measurementItems.get(0).getMeasurementItemFields()?????????????????????
        List<MeasurementItemField> measurementItemFields = measurementItems.get(0).getMeasurementItemFields();

        attributes.put("deviceId", deviceId);
        attributes.put("measurementTemplateId", measurementTemplateId);
        attributes.put("measurementItems", measurementItems);
        attributes.put("measurementItemFields", measurementItemFields);
        return new ModelAndView("/backstage/measurement-task-detail/measurement-task-detail-charts", attributes);
    }

    /**
     * ???????????????????????????????????????
     */
    @RequestMapping("/chartsData")
    @ResponseBody
    public Json getChartsData() throws Exception {
        String deviceId = req.getParameter("deviceId");
        String[] measurementItemIds = req.getParameterMap().get("measurementItemIds[]");
        String[] associationCodes = req.getParameterMap().get("associationCodes[]");
        String measurementTemplateId = req.getParameter("measurementTemplateId");
//        String startTimeStr = req.getParameter("startTimeStr");
//        String endTimeStr = req.getParameter("endTimeStr");

//        Timestamp startTime = DateUtil.toTimestamp(startTimeStr, "yyyy-MM-dd");
//        Timestamp endTime = DateUtil.toTimestamp(endTimeStr, "yyyy-MM-dd");

        if (measurementItemIds == null || associationCodes == null)
            return new Json(JsonMessage.SUCCESS, new ArrayList<>());

        Map<String, Object> params = new HashMap<>();

        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT\n");
        sqlBuilder.append("  fieldValue.id as id,\n");
        sqlBuilder.append("  fieldValue.add_time as addTime,\n");
        sqlBuilder.append("  fieldValue.value as value,\n");
        sqlBuilder.append("  fieldValue.measurement_item_field_id as measurementItemFieldId,\n");
        sqlBuilder.append("  fieldValue.device_measurement_item_id as deviceMeasurementItemId,\n");
        sqlBuilder.append("  itemField.name as measurementItemFieldName,\n");
        sqlBuilder.append("  item.name as measurementItemName\n");
        sqlBuilder.append("FROM\n");
        sqlBuilder.append("  measurement_item_field_value fieldValue\n");
        sqlBuilder.append("  LEFT JOIN measurement_item_field itemField ON itemField.id = fieldValue.measurement_item_field_id\n");
        sqlBuilder.append("  LEFT JOIN measurement_item item ON item.id = itemField.measurement_item_id\n");
        sqlBuilder.append("  LEFT JOIN measurement_template template ON template.id = item.measurement_template_id\n");
        sqlBuilder.append("WHERE\n");
        sqlBuilder.append("  fieldValue.device_id = :deviceId\n");
        sqlBuilder.append("  AND itemField.association_code in :associationCodes");
        sqlBuilder.append("  AND item.id in :measurementItemIds");
        sqlBuilder.append("  AND template.id = :measurementTemplateId");
//        sqlBuilder.append("  AND fieldValue.add_time > :startTime");
//        sqlBuilder.append("  AND fieldValue.add_time < :endTime");

        params.put("deviceId", deviceId);
        params.put("associationCodes", associationCodes);
        params.put("measurementItemIds", measurementItemIds);
        params.put("measurementTemplateId", measurementTemplateId);
//        params.put("startTime", startTime);
//        params.put("endTime", endTime);

        String sql = sqlBuilder.toString();
        List<MeasurementItemFieldValue> measurementItemFieldValues = measurementItemFieldValueBaseService.findSQL(
                sql, params, MeasurementItemFieldValue.class, 1, 999999);

        Map<String, List<MeasurementItemFieldValue>> collect =
                measurementItemFieldValues.stream()
                        .collect(Collectors.groupingBy(x -> x.getMeasurementItemId() + x.getMeasurementItemFieldId()));

        List<MeasurementItemFieldValueChatsPojo> deviceMaintenanceParameterChatsPojos =
                collect.values().stream().map(MeasurementItemFieldValueChatsPojo::new).collect(Collectors.toCollection(LinkedList::new));

        return new Json(JsonMessage.SUCCESS, deviceMaintenanceParameterChatsPojos);
    }

    @RequestMapping("/exportMeasurementTaskDetail")
    @ResponseBody
    public void exportMeasurementTaskDetail() throws Exception {
        String deviceId = req.getParameter("deviceId");
        String measurementTemplateId = req.getParameter("measurementTemplateId");

        MeasurementTemplate measurementTemplate = measurementTemplateService.getById(measurementTemplateId);
        String templateName = measurementTemplate.getName();

        if (StringUtils.isEmpty(deviceId))
            throw new CustomException(new Json(JsonMessage.SYS_ERROR));

        Device device = deviceService.getById(deviceId);

        if (StringUtils.isEmpty(measurementTemplateId))
            throw new CustomException(JsonMessage.SYS_ERROR, "???????????????");

        // ??????????????????
        List<MeasurementItem> measurementItems = measurementTemplateService.getItemsAndItemFieldsByTemplateId(measurementTemplateId);

        // ???????????????????????????measurementItem?????????measurementField???????????????
        List<MeasurementItemField> fieldNames = new LinkedList<>(measurementItems.get(0).getMeasurementItemFields());

        // ??????????????????????????????????????????
        Map<String, Object> params = new HashMap<>();
        Map<String, String> conditions = new HashMap<>();
        params.put("deviceId", deviceId);
        conditions.put("deviceId", "=");
        params.put("measurementTemplateId", measurementTemplateId);
        conditions.put("measurementTemplateId", "=");
        params.put("finishedStatus", MeasurementTaskDetail.COMPLETED);
        conditions.put("finishedStatus", "=");
        List<MeasurementTaskDetail> measurementTaskDetails = measurementTaskDetailBaseService.getObjcetPagination(MeasurementTaskDetail.class,
                params, conditions, 1, 999999, "order by executeTime desc");

        if (measurementTaskDetails.isEmpty()) {
            List<String> tableTitles = new ArrayList<>();
            tableTitles.add("????????????");
            tableTitles.add("?????????");
            tableTitles.add("??????");
            tableTitles.addAll(StreamUtil.getList(fieldNames, MeasurementItemField::getName));
            tableTitles.add("?????????");

            Map<Integer, ExcelRowInfo> excelRowInfoMap = new HashMap<>();
            int rows = 0;
            excelRowInfoMap.put(rows++, new ExcelRowInfo(rows, templateName));
            // ??????
            excelRowInfoMap.put(rows++, new ExcelRowInfo(rows, tableTitles));

            ExcelSheetInfo excelSheetInfo = new ExcelSheetInfo(templateName, excelRowInfoMap);
            Workbook excelFile = ExcelUtil.createExcelFile(List.of(excelSheetInfo));
            MultipartFile workbookFile = ExcelUtil.workbook2File(excelFile, templateName);
            export(workbookFile);
        }

        // ???????????????
        // key:measuermeentTask.id(??????id) value:???????????????
        Map<String, String> executeUserNameMap = measurementTaskDetailService.getExecuteUserNames(measurementTaskDetails);
        for (MeasurementTaskDetail measurementTaskDetail : measurementTaskDetails) {
            String taskId = measurementTaskDetail.getMeasurementTaskId();
            String executeUserNames = executeUserNameMap.get(taskId);
            measurementTaskDetail.setExecuteUserNames(executeUserNames);
        }

        List<String> measurementTaskDetailIds = StreamUtil.getList(measurementTaskDetails, MeasurementTaskDetail::getId);

        // ?????????????????????????????????
        // key:???????????????id???value:?????????id??????????????????
        Map<String, List<MeasurementItemFieldValue>> measurementItemFieldValueMap =
                measurementItemFieldValueService.getMeasurementItemFieldValues(measurementTemplateId, measurementTaskDetailIds);

        // ??????executeUserNameMap????????????????????????
        for (MeasurementTaskDetail measurementTaskDetail : measurementTaskDetails) {
            String measurementTaskId = measurementTaskDetail.getMeasurementTaskId();

            // ???model??????????????????
            String executeUserNames = executeUserNameMap.get(measurementTaskId);
            measurementTaskDetail.setExecuteUserNames(executeUserNames);
        }

        List<MeasurementTaskDetail> values = new ArrayList<>();
        // ??????measurementItemFieldValueMap????????????????????????
        for (MeasurementTaskDetail measurementTaskDetail : measurementTaskDetails) {
            String measurementTaskDetailId = measurementTaskDetail.getId();
            // measurementItemFieldValueMap.get(measurementTaskDetailId)????????????????????????????????????
            List<MeasurementItemFieldValue> measurementItemFieldValueList = measurementItemFieldValueMap.get(measurementTaskDetailId);

            // key:????????????id(measurementItemFieldId), value:?????????
            Map<String, MeasurementItemFieldValue> fieldValueMap = measurementItemFieldValueList.stream()
                    .collect(Collectors.toMap(MeasurementItemFieldValue::getMeasurementItemFieldId, x -> x));

            // ?????????????????????????????????????????????????????????????????????????????????
            for (MeasurementItem measurementItem : measurementItems) {
                MeasurementTaskDetail detail = new MeasurementTaskDetail();
                BeanUtils.copyProperties(measurementTaskDetail, detail);
                detail.setMeasurementItemName(measurementItem.getName());
                List<MeasurementItemFieldValue> measurementItemFieldValues = new ArrayList<>();
                for (MeasurementItemField measurementItemField : measurementItem.getMeasurementItemFields()) {
                    MeasurementItemFieldValue measurementItemFieldValue = fieldValueMap.get(measurementItemField.getId());
                    if (measurementItemFieldValue != null)
                        measurementItemFieldValues.add(measurementItemFieldValue);
                }
                // ???????????????????????????
                if (measurementItemFieldValues.isEmpty())
                    continue;
                detail.setMeasurementItemFieldValues(measurementItemFieldValues);
                values.add(detail);
            }
        }

        List<String> tableTitles = new ArrayList<>();
        tableTitles.add("????????????");
        tableTitles.add("?????????");
        tableTitles.add("?????????(??????)");
        tableTitles.addAll(StreamUtil.getList(fieldNames, MeasurementItemField::getName));
        tableTitles.add("?????????");

        Map<Integer, ExcelRowInfo> excelRowInfoMap = new HashMap<>();
        int rows = 0;
        excelRowInfoMap.put(rows++, new ExcelRowInfo(rows, templateName));
        // ??????
        excelRowInfoMap.put(rows++, new ExcelRowInfo(rows, tableTitles));
        for (MeasurementTaskDetail measurementTaskDetail : values) {
            List<String> list = new ArrayList<>();
            list.add(DateUtil.format(measurementTaskDetail.getExecuteTime()));
            list.add(device.getName());
            list.add(measurementTaskDetail.getMeasurementItemName());
            List<MeasurementItemFieldValue> measurementItemFieldValues = measurementTaskDetail.getMeasurementItemFieldValues();
            list.addAll(StreamUtil.getList(measurementItemFieldValues, MeasurementItemFieldValue::getValue));
            list.add(measurementTaskDetail.getExecuteUserNames());
            ExcelRowInfo excelRowInfo = new ExcelRowInfo(rows++, list);
            excelRowInfoMap.put(rows++, excelRowInfo);
        }

        ExcelSheetInfo excelSheetInfo = new ExcelSheetInfo(templateName, excelRowInfoMap);
        Workbook excelFile = ExcelUtil.createExcelFile(List.of(excelSheetInfo));
        MultipartFile workbookFile = ExcelUtil.workbook2File(excelFile, templateName);
        export(workbookFile);
    }
}
