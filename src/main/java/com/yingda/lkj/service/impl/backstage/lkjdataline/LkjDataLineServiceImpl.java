package com.yingda.lkj.service.impl.backstage.lkjdataline;

import com.yingda.lkj.beans.Constant;
import com.yingda.lkj.beans.entity.backstage.dataversion.DataApproveFlow;
import com.yingda.lkj.beans.entity.backstage.dataversion.DataVersion;
import com.yingda.lkj.beans.entity.backstage.device.Device;
import com.yingda.lkj.beans.entity.backstage.device.DeviceExtendValues;
import com.yingda.lkj.beans.entity.backstage.line.Fragment;
import com.yingda.lkj.beans.entity.backstage.lkj.LkjDataLine;
import com.yingda.lkj.beans.entity.backstage.line.RailwayLine;
import com.yingda.lkj.beans.entity.backstage.line.Station;
import com.yingda.lkj.beans.entity.backstage.lkj.LkjGroup;
import com.yingda.lkj.beans.entity.backstage.lkj.LkjTask;
import com.yingda.lkj.beans.entity.backstage.organization.Organization;
import com.yingda.lkj.beans.enums.dataversion.ApproveDataType;
import com.yingda.lkj.beans.exception.CustomException;
import com.yingda.lkj.beans.pojo.lkj.LkjDataLineFromExcel;
import com.yingda.lkj.beans.pojo.device.Semaphore;
import com.yingda.lkj.beans.pojo.device.SemaphoreFromExcel;
import com.yingda.lkj.beans.system.Json;
import com.yingda.lkj.beans.system.JsonMessage;
import com.yingda.lkj.beans.system.Pair;
import com.yingda.lkj.dao.BaseDao;
import com.yingda.lkj.service.backstage.dataversion.DataVersionService;
import com.yingda.lkj.service.backstage.device.DeviceExtendValuesService;
import com.yingda.lkj.service.backstage.device.SemaphoreTypeService;
import com.yingda.lkj.service.backstage.lkjdataline.LkjDataLineService;
import com.yingda.lkj.service.backstage.lkjdataline.LkjGroupService;
import com.yingda.lkj.service.backstage.lkjtask.LkjTaskService;
import com.yingda.lkj.service.backstage.organization.OrganizationClientService;
import com.yingda.lkj.utils.StreamUtil;
import com.yingda.lkj.utils.StringUtils;
import com.yingda.lkj.utils.pojo.CollectUtils;
import com.yingda.lkj.utils.pojo.backstage.LkjDataLineUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author hood  2020/1/7
 */
@Service("lkjDataLineService")
public class LkjDataLineServiceImpl implements LkjDataLineService {

    @Autowired
    private BaseDao<Device> deviceBaseDao;
    @Autowired
    private BaseDao<RailwayLine> railwayLineBaseDao;
    @Autowired
    private BaseDao<Station> stationBaseDao;
    @Autowired
    private OrganizationClientService organizationClientService;
    @Autowired
    private DeviceExtendValuesService deviceExtendValuesService;
    @Autowired
    private SemaphoreTypeService semaphoreTypeService;
    @Autowired
    private BaseDao<LkjDataLine> lkjDataLineBaseDao;
    @Autowired
    private BaseDao<Fragment> fragmentBaseDao;
    @Autowired
    private LkjGroupService lkjGroupService;
    @Autowired
    private BaseDao<LkjGroup> lkjGroupBaseDao;
    @Autowired
    private LkjTaskService lkjTaskService;
    @Autowired
    private DataVersionService dataVersionService;

    @Override
    public List<Semaphore> expandLkjDataLine(List<LkjDataLine> lkjDataLines) throws Exception {
        return fillLkjDataLine(lkjDataLines).secondValue;
    }

    @Override
    public List<LkjDataLine> fillLkjDataLineDevice(List<LkjDataLine> lkjDataLines) throws Exception {
        return fillLkjDataLine(lkjDataLines).firstValue;
    }

    private Pair<List<LkjDataLine>, List<Semaphore>> fillLkjDataLine(List<LkjDataLine> lkjDataLines) throws Exception {
//        lkjDataLines = new ArrayList<>(lkjDataLines).stream().sorted(Comparator.comparingInt(LkjDataLine::getSeq)).collect(Collectors.toList());
//
//        List<Semaphore> semaphores = new ArrayList<>();
//
//        List<String> deviceIds = new ArrayList<>();
//        for (LkjDataLine lkjDataLine : lkjDataLines) {
//            String leftDeviceId = lkjDataLine.getLeftDeviceId();
//            String rightDeviceId = lkjDataLine.getRightDeviceId();
//
//            deviceIds.add(leftDeviceId);
//            deviceIds.add(rightDeviceId);
//        }
//
//        // ????????????
//        List<Device> devices = deviceBaseDao.find("from Device where id in :deviceIds", Map.of("deviceIds", deviceIds));
//        List<String> stationIds = devices.stream().map(Device::getStationId).collect(Collectors.toList());
//        List<String> railwayLineIds = devices.stream().map(Device::getRailwayLineId).collect(Collectors.toList());
//        List<String> fragmentIds = lkjDataLines.stream().map(LkjDataLine::getFragmentId).collect(Collectors.toList());
//        List<String> ljkGroupIds = lkjDataLines.stream().map(LkjDataLine::getLkjGroupId).collect(Collectors.toList());
//        // ??????
//        List<Station> stations = stationBaseDao.find("from Station where id in :stationIds", Map.of("stationIds", stationIds));
//        // ??????
//        List<RailwayLine> railwayLines = railwayLineBaseDao.find("from RailwayLine where id in :railwayLineIds", Map.of("railwayLineIds", railwayLineIds));
//        // ??????
//        List<Fragment> fragments = fragmentBaseDao.find("from Fragment where id in :fragmentIds", Map.of("fragmentIds", fragmentIds));
//        // ??????
//        List<LkjGroup> groups = lkjGroupBaseDao.find("from LkjGroup where id in :lkjGroupsIds", Map.of("lkjGroupsIds", ljkGroupIds));
//
//        Map<String, Device> deviceMap = devices.stream().collect(Collectors.toMap(Device::getId, x -> x));
//        Map<String, Station> stationMap = stations.stream().collect(Collectors.toMap(Station::getId, x -> x));
//        Map<String, RailwayLine> railwayLineMap = railwayLines.stream().collect(Collectors.toMap(RailwayLine::getId, x -> x));
//        Map<String, Fragment> fragmentMap = fragments.stream().collect(Collectors.toMap(Fragment::getId, x -> x));
//        Map<String, LkjGroup> lkjGroupMap = groups.stream().collect(Collectors.toMap(LkjGroup::getId, x -> x));
//        // ??????????????????
//        Map<String, String> semaphoreTypeNames = semaphoreTypeService.getSemaphoreTypeNames();
//        // ?????????????????????
//        deviceExtendValuesService.getExtendValues(devices);
//
//        for (LkjDataLine lkjDataLine : lkjDataLines) {
//            Fragment fragment = fragmentMap.get(lkjDataLine.getFragmentId());
//            lkjDataLine.setFragmentName(Optional.ofNullable(fragment).map(Fragment::getName).orElse(""));
//
//            LkjGroup lkjGroup = lkjGroupMap.get(lkjDataLine.getLkjGroupId());
//            lkjDataLine.setLkjGroupName(lkjGroup.getName());
//
//            Device leftDevice = deviceMap.get(lkjDataLine.getLeftDeviceId());
//            Semaphore leftSemaphore = createSemaphoreByDeviceId(leftDevice, stationMap, lkjDataLine, railwayLineMap, semaphoreTypeNames);
//            semaphores.add(leftSemaphore);
//            lkjDataLine.setLeftSemaphore(leftSemaphore);
//
//            Device rightDevice = deviceMap.get(lkjDataLine.getRightDeviceId());
//            Semaphore rightSemaphore = createSemaphoreByDeviceId(rightDevice, stationMap, lkjDataLine, railwayLineMap, semaphoreTypeNames);
//            semaphores.add(rightSemaphore);
//            lkjDataLine.setRightSemaphore(rightSemaphore);
//        }
//
//        semaphores = filledDistance(semaphores);

        return new Pair<>(lkjDataLines, null);
    }

    @Override
    public List<LkjDataLine> createLkjDataLine(DataApproveFlow dataApproveFlow, List<LkjDataLine> rawLkjDataLines) {
        rawLkjDataLines = fillUniqueCode(rawLkjDataLines);

        List<LkjDataLine> lkjDataLines = new ArrayList<>();
        for (LkjDataLine rawLkjDataLine : rawLkjDataLines) {
            // ???????????????????????????????????????uniqueCode???????????????????????????rawLkjDataLine??????uniqueCode????????????????????????fillUniqueCode??????????????????????????????
            lkjDataLines.add(new LkjDataLine(rawLkjDataLine, dataApproveFlow, rawLkjDataLine.getUniqueCode()));
        }

        lkjDataLineBaseDao.bulkInsert(lkjDataLines, 100);
        lkjGroupService.add(lkjDataLines); // ????????????
        return lkjDataLines;
    }


    @Override
    public List<LkjDataLine> createLkjDataLine(LkjTask lkjTask, List<LkjDataLine> rawLkjDataLines) {
        rawLkjDataLines = fillUniqueCode(rawLkjDataLines);

        List<LkjDataLine> lkjDataLines = new ArrayList<>();
        for (LkjDataLine rawLkjDataLine : rawLkjDataLines) {
            // ???????????????????????????????????????uniqueCode???????????????????????????rawLkjDataLine??????uniqueCode????????????????????????fillUniqueCode??????????????????????????????
            lkjDataLines.add(new LkjDataLine(rawLkjDataLine, lkjTask, rawLkjDataLine.getUniqueCode()));
        }

        lkjDataLineBaseDao.bulkInsert(lkjDataLines, 100);
        lkjGroupService.add(lkjDataLines); // ????????????
        return lkjDataLines;
    }

    @Override
    public List<LkjDataLine> wrapLkjDataLine(List<LkjDataLineFromExcel> lkjDataLineFromExcels, String fragmentId) throws CustomException {
        List<LkjDataLine> lkjDataLines = new ArrayList<>();

        for (LkjDataLineFromExcel lkjDataLineFromExcel : lkjDataLineFromExcels) {
            LkjDataLine lkjDataLine = new LkjDataLine();
            String leftDeviceId = getDeviceId(lkjDataLineFromExcel.getLeftNode());
            String rightDeviceId = getDeviceId(lkjDataLineFromExcel.getRightNode());

            String fragmentIdFromExcel = lkjDataLineFromExcel.getFragmentId();
            // ??????lkjDataLineFromExcel????????????fragmentId?????????lkjDataLineFromExcel??????fragmentId(??????????????????excel????????????excel????????????????????????)
            fragmentId = StringUtils.isNotEmpty(fragmentIdFromExcel) ? fragmentIdFromExcel : fragmentId;

            lkjDataLine.setFragmentId(fragmentId);
            lkjDataLine.setLeftDeviceId(leftDeviceId);
            lkjDataLine.setRightDeviceId(rightDeviceId);
            lkjDataLine.setDistance(lkjDataLineFromExcel.getDistance());
            lkjDataLine.setDownriver(lkjDataLineFromExcel.getDownriver());
            lkjDataLine.setRetrograde(lkjDataLineFromExcel.getRetrograde());
            lkjDataLine.setUniqueCode(lkjDataLineFromExcel.getUniqueCode());
            lkjDataLine.setSeq(lkjDataLineFromExcel.getSeq());
            lkjDataLine.setTableType(lkjDataLineFromExcel.getTableType());

            lkjDataLines.add(lkjDataLine);
        }

        return lkjDataLines;
    }

    @Override
    public void fillTask(String lkjTaskId, List<LkjDataLine> rawLkjDataLines) {
        List<LkjDataLine> pendingFilleds = lkjDataLineBaseDao.find("from LkjDataLine where lkjTaskId = :lkjTaskId", Map.of("lkjTaskId", lkjTaskId));

        // key: ???downriver???retrograde???leftDeviceId???rightDeviceId??????
        Map<String, LkjDataLine> pendingFilledMap = pendingFilleds.stream()
                .collect(Collectors.toMap(LkjDataLineUtils::createUniqueCode, x -> x));

        for (LkjDataLine lkjDataLine : rawLkjDataLines) {
            String uniqueCode = LkjDataLineUtils.createUniqueCode(lkjDataLine);
            double distance = lkjDataLine.getDistance();

            LkjDataLine pendingFilled = pendingFilledMap.get(uniqueCode);
            pendingFilled.setDistance(distance);
        }

        lkjDataLineBaseDao.bulkInsert(pendingFilleds);
    }

    @Override
    public void refuseLkjDataLines(DataApproveFlow dataApproveFlow) {
        Timestamp current = new Timestamp(System.currentTimeMillis());

        // ???????????????????????????lkj??????
        List<LkjDataLine> lkjDataLines = lkjDataLineBaseDao.find(
                "from LkjDataLine where dataApproveFlowId = :dataApproveFlowId order by uniqueCode, addTime desc",
                Map.of("dataApproveFlowId", dataApproveFlow.getId())
        );

        // 2.?????????????????????????????????????????????
        for (LkjDataLine lkjDataLine : lkjDataLines) {
            lkjDataLine.setUpdateTime(current);
            lkjDataLine.setApproveStatus(LkjDataLine.FAILED);
            lkjDataLine.setOutdated(LkjDataLine.OUTDATED);
        }
        lkjDataLineBaseDao.bulkInsert(lkjDataLines);

        // 3.???????????????lkj??????????????????????????????????????????
        List<String> lkjTaskIds = StreamUtil.getList(lkjDataLines, LkjDataLine::getLkjTaskId);
        lkjTaskService.refuseLkjTasks(lkjTaskIds);
    }

    @Override
    public void completeLkjDataLine(DataApproveFlow dataApproveFlow) {
        String rawDataApproveFlowId = dataApproveFlow.getId();
        Timestamp current = new Timestamp(System.currentTimeMillis());

        // ????????????lkj??????,????????????lkj??????????????????
        // ??????????????????lkj??????
        List<LkjDataLine> lkjDataLines = lkjDataLineBaseDao.find(
                "from LkjDataLine where dataApproveFlowId = :dataApproveFlowId",
                Map.of("dataApproveFlowId", rawDataApproveFlowId)
        );
        // ???????????????
        List<String> uniqueCodes = lkjDataLines.stream().map(LkjDataLine::getUniqueCode).distinct().collect(Collectors.toList());

        // ?????????????????????????????????????????????????????????
        List<LkjDataLine> theOutdated = lkjDataLineBaseDao.find(
                "from LkjDataLine where uniqueCode in :uniqueCodes and outdated = :outdated and approveStatus = :approveStatus",
                Map.of("uniqueCodes", uniqueCodes, "outdated", LkjDataLine.USING, "approveStatus", LkjDataLine.APPROVED)
        );

        // ????????????????????????
        for (LkjDataLine lkjDataLine : theOutdated) {
            lkjDataLine.setUpdateTime(current);
            lkjDataLine.setOutdated(LkjDataLine.OUTDATED);
        }
        lkjDataLineBaseDao.bulkInsert(theOutdated);

        // ???????????????????????????????????????
        for (LkjDataLine lkjDataLine : lkjDataLines) {
            lkjDataLine.setUpdateTime(current);
            lkjDataLine.setApproveStatus(LkjDataLine.APPROVED);
            lkjDataLine.setOutdated(LkjDataLine.USING);
        }
        lkjDataLineBaseDao.bulkInsert(lkjDataLines);

        // 3.???????????????lkj?????????????????????(lkj_task)???????????????????????????
        List<String> lkjTaskIds = lkjDataLines.stream().map(LkjDataLine::getLkjTaskId).distinct().collect(Collectors.toList());
        lkjTaskService.completeLkjTasks(lkjTaskIds);

        // 4.????????????lkj????????????(dataVersion)
        dataVersionService.createInterimDataVersion(dataApproveFlow.getSectionId(), ApproveDataType.LKJ14, lkjDataLines, theOutdated);
    }

    /**
     * ?????????uniqueCode?????????lkj??????list??????????????????unqueCode
     */
    private List<LkjDataLine> fillUniqueCode(List<LkjDataLine> rawLkjDataLines) {
        // ?????????rawLkjDataLines.uniqueCode??????
        // key: uniqueCode, value: uniqueCode?????????lkj??????list
        Map<String, List<LkjDataLine>> lkjDataLineMap = new HashMap<>();

        for (LkjDataLine rawLkjDataLine : rawLkjDataLines) {
            String temporaryUniqueCode = rawLkjDataLine.getUniqueCode();
            List<LkjDataLine> sameUniqueCodeData = lkjDataLineMap.get(temporaryUniqueCode);

            if (sameUniqueCodeData == null)
                sameUniqueCodeData = new ArrayList<>();

            sameUniqueCodeData.add(rawLkjDataLine);
            lkjDataLineMap.put(temporaryUniqueCode, sameUniqueCodeData);
        }

        // ????????????????????????uniqueCode??????lkjDataLines???
        for (List<LkjDataLine> lkjDataLines : lkjDataLineMap.values()) {
            String persistentUniqueCode = getUniqueCode(lkjDataLines);
            lkjDataLines.forEach(lkjDataLine -> lkjDataLine.setUniqueCode(persistentUniqueCode));
        }

        List<LkjDataLine> result = new ArrayList<>();
        lkjDataLineMap.values().forEach(result::addAll);

        return result;
    }

    /**
     * ??????SemaphoreFromExcel?????????????????????id??????????????????lkj???????????????????????????????????????
     */
    private String getDeviceId(SemaphoreFromExcel semaphoreFromExcel) throws CustomException {
        String railwayLineCode = semaphoreFromExcel.getRailwayLineCode();
        String stationName = semaphoreFromExcel.getStationName();
        String code = semaphoreFromExcel.getCode();

        Map<String, Object> params = new HashMap<>();
        StringBuilder sqlBuilder = new StringBuilder()
                .append("SELECT\n")
                .append("  device.id \n")
                .append("FROM\n")
                .append("  device\n")
                .append("  LEFT JOIN station ON  station.id = device.station_id\n")
                .append("  LEFT JOIN railway_line line ON line.id = device.railway_line_id \n")
                .append("WHERE\n")
                .append("  line.code = :railwayLineCode \n")
                .append("  AND station.NAME = :stationName \n")
                .append("  AND device.code = :code")
                .append("  AND device.hide = :hide");
        params.put("railwayLineCode", railwayLineCode);
        params.put("stationName", stationName);
        params.put("code", code);
        params.put("hide", Constant.SHOW);

        List<Device> deviceList = deviceBaseDao.findSQL(sqlBuilder.toString(), params, Device.class, 1, 1);
        if (deviceList.size() == 0)
            throw new CustomException(new Json(JsonMessage.PARAM_INVALID, "????????????" + semaphoreFromExcel.getExcelLineNumber() + "???????????????????????????"));

        return deviceList.get(0).getId();
    }

    /**
     * ???uniqueCode??????????????????semaphore???distanceStype(???????????????)???????????????????????????????????????
     */
    private List<Semaphore> filledDistance(List<Semaphore> semaphores) {
        List<Semaphore> result = new ArrayList<>();

        // uniqueCode??????????????????semaphore
        List<List<Semaphore>> semaphoresList = CollectUtils.groupList(Semaphore::getUniqueCode, semaphores);

        // ???????????????id
        for (List<Semaphore> iter : semaphoresList) {
            List<String> distanceStyles = iter.stream().filter(StreamUtil.distinct(Semaphore::getLkjDataLineId)).map(Semaphore::getDistanceStr).collect(Collectors.toList());
            distanceStyles.add(UUID.randomUUID().toString()); // ????????????????????????lkj,?????????uuid
            iter.forEach(x -> x.setDistanceStyle(distanceStyles));

            List<String> lkjDataLineIds = iter.stream().map(Semaphore::getLkjDataLineId).distinct().collect(Collectors.toList());
            lkjDataLineIds.add(UUID.randomUUID().toString()); // ????????????????????????lkj,?????????uuid
            iter.forEach(x -> x.setLkjDataLineIds(lkjDataLineIds));

            result.addAll(iter.stream().distinct().collect(Collectors.toList()));
        }

        return result;
    }

    /**
     * ????????????(??????lkj????????????????????????????????????????????????)
     * ??????/?????? + ??????/?????? + ?????????????????????id ???'-'??????
     */
    private String getUniqueCode(List<LkjDataLine> lkjDataLines) {
        byte downriver = lkjDataLines.get(0).getDownriver(); // ???????????????
        byte retrograde = lkjDataLines.get(0).getRetrograde();

        List<String> deviceIds = new ArrayList<>();
        for (LkjDataLine lkjDataLine : lkjDataLines) {
            deviceIds.add(lkjDataLine.getLeftDeviceId());
            deviceIds.add(lkjDataLine.getRightDeviceId());
        }
        deviceIds = deviceIds.stream().distinct().collect(Collectors.toList());

        return downriver + "-" + retrograde + "-" + String.join("-", deviceIds);
    }

    private Semaphore createSemaphoreByDeviceId(Device device, Map<String, Station> stationMap, LkjDataLine lkjDataLine,
                                                Map<String, RailwayLine> railwayLineMap, Map<String, String> semaphoreTypeNames) throws CustomException {
//        Station station = stationMap.get(device.getStationId());
//        if (station == null)
//            throw new CustomException(new Json(JsonMessage.SYS_ERROR, "?????????id = " + device.getStationId() + "?????????", "?????????????????????????????????"));
//
//        Organization bureau = organizationClientService.getBureauByWorkareaId(station.getWorkAreaId());
//        RailwayLine railwayLine = railwayLineMap.get(device.getRailwayLineId());
//
//        List<DeviceExtendValues> extendValues = device.getExtendValues();
//
//        if (extendValues == null)
//            throw new CustomException(new Json(JsonMessage.SYS_ERROR, device.getName()));
//
//
//        // key: ?????????????????? value??????????????????
//        Map<String, String> values = new HashMap<>();
//        for (DeviceExtendValues extendValue : extendValues) {
//            String deviceFieldId = extendValue.getDeviceFieldId();
//            String key = semaphoreTypeNames.get(deviceFieldId);
//            String fieldValue = extendValue.getFieldValue();
//            values.put(key, fieldValue);
//        }
//
//        return new Semaphore(device, lkjDataLine, bureau, railwayLine, station, values);
        return null;
    }
}
