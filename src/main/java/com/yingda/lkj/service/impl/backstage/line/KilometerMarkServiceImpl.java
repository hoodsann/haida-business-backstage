package com.yingda.lkj.service.impl.backstage.line;

import com.yingda.lkj.beans.entity.backstage.line.KilometerMark;
import com.yingda.lkj.beans.entity.backstage.line.RailwayLineSection;
import com.yingda.lkj.beans.entity.backstage.location.Location;
import com.yingda.lkj.beans.exception.CustomException;
import com.yingda.lkj.beans.system.JsonMessage;
import com.yingda.lkj.beans.system.Pair;
import com.yingda.lkj.dao.BaseDao;
import com.yingda.lkj.service.backstage.line.KilometerMarkService;
import com.yingda.lkj.service.backstage.line.RailwayLineSectionService;
import com.yingda.lkj.service.backstage.line.StationService;
import com.yingda.lkj.service.backstage.location.LocationService;
import com.yingda.lkj.utils.StreamUtil;
import com.yingda.lkj.utils.location.LocationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service("kilometerMarkService")
public class KilometerMarkServiceImpl implements KilometerMarkService {

    @Autowired
    private BaseDao<KilometerMark> kilometerMarkBaseDao;
    @Autowired
    private RailwayLineSectionService railwayLineSectionService;
    @Autowired
    private LocationService locationService;
    @Autowired
    private StationService stationService;

    @Override
    public KilometerMark getByRailwaylineSectionIdAndStationId(String railwayLineSectionId, String stationId) {
        return kilometerMarkBaseDao.get(
                "from KilometerMark where railwayLineSectionId = :railwayLineSectionId and stationId = :stationId",
                Map.of("railwayLineSectionId", railwayLineSectionId, "stationId", stationId)
        );
    }

    @Override
    public List<KilometerMark> getByRailwayLineSectionId(String railwayLineSectionId) {
        return kilometerMarkBaseDao.find(
                "from KilometerMark where railwayLineSectionId = :railwayLineSectionId",
                Map.of("railwayLineSectionId", railwayLineSectionId)
        );
    }

    @Override
    public List<KilometerMark> getByRailwayLineSections(List<RailwayLineSection> railwayLineSections) {
        List<String> railwayLineSectionIds = StreamUtil.getList(railwayLineSections, RailwayLineSection::getId);
        return kilometerMarkBaseDao.find(
                "from KilometerMark where railwayLineSectionId in :railwayLineSectionIds",
                Map.of("railwayLineSectionIds", railwayLineSectionIds)
        );
    }

    @Override
    public void calculateKilometerMarksInLine(String startStationId, String endStationId, String railwayLineId) throws CustomException {
        RailwayLineSection railwayLineSection = railwayLineSectionService.getByStationIdAndRailwayLineId(startStationId, endStationId, railwayLineId);
        List<Location> railwayLineSectionLocations = locationService.getByDataId(railwayLineSection.getId());

        // ????????????????????????????????????
        Pair<KilometerMark, KilometerMark> startKilometerMarkAndEndKilometerMark = createStartEndKilometerMark(
                startStationId, endStationId, railwayLineSection, railwayLineSectionLocations
        );

        // ???????????????????????????????????????????????????????????????????????????
        KilometerMark startKilometerMark = startKilometerMarkAndEndKilometerMark.getFirstValue();
        KilometerMark endKilometerMark = startKilometerMarkAndEndKilometerMark.getSecondValue();

        List<Location> startKilometerMarkLocations = locationService.getByDataId(startKilometerMark.getId());
        if (startKilometerMarkLocations.isEmpty())
            throw new CustomException(JsonMessage.DATA_NO_COMPLETE, String.format("??????%s????????????????????????????????????", stationService.getById(startStationId).getName()));

        Location startKilometerMarkLocation = startKilometerMarkLocations.get(0);


        // ??????????????????????????????
        Location startLocation = railwayLineSectionLocations.stream()
                .filter(x -> x.getLongitude() == startKilometerMarkLocation.getLongitude() && x.getLatitude() == startKilometerMarkLocation.getLatitude())
                .reduce(null, (x, y) -> y);
        int startIndex = railwayLineSectionLocations.indexOf(startLocation);
        if (startIndex == -1)
            throw new CustomException(JsonMessage.DATA_NO_COMPLETE, String.format("?????????????????????%s????????????????????????????????????",
                    stationService.getById(startStationId).getName()));

        // ?????????????????????index
        List<Location> endKilometerMarkLocations = locationService.getByDataId(endKilometerMark.getId());
        if (endKilometerMarkLocations.isEmpty())
            throw new CustomException(JsonMessage.DATA_NO_COMPLETE, String.format("??????%s????????????????????????????????????",
                    stationService.getById(endStationId).getName()));
        Location endKilometerMarkLocation = endKilometerMarkLocations.get(0);

        Location endLocationOnLine = railwayLineSectionLocations.stream()
                .filter(x -> x.getLongitude() == endKilometerMarkLocation.getLongitude() && x.getLatitude() == endKilometerMarkLocation.getLatitude())
                .reduce(null, (x, y) -> y);
        int endIndex = railwayLineSectionLocations.indexOf(endLocationOnLine);
        if (endIndex == -1)
            throw new CustomException(JsonMessage.DATA_NO_COMPLETE, String.format("?????????????????????%s????????????????????????????????????",
                    stationService.getById(endStationId).getName()));

    }

    /**
     * ????????????????????????????????????
     *
     * @param startStationId       ?????????
     * @param endStationId         ?????????
     * @param railwayLineSection ????????????
     * @return Pair.firstValue:??????????????????Pair.secondValue:???????????????
     */
    public Pair<KilometerMark, KilometerMark> createStartEndKilometerMark(RailwayLineSection railwayLineSection, String startStationId, String endStationId, Location startLocation, Location endLocation) {
        String railwayLineSectionId = railwayLineSection.getId();

        KilometerMark startKilometerMark = new KilometerMark(railwayLineSectionId, startStationId, 0);
        KilometerMark endKilometerMark = new KilometerMark(railwayLineSectionId, endStationId, 0);

        List<Location> railwayLineSectionLocations = railwayLineSection.getLocations();
        // ???????????????????????????railwayLineSectionLocations????????????????????????
        railwayLineSectionLocations = railwayLineSectionLocations.stream()
                .dropWhile(x -> !locationService.samePoint(x, startLocation))
                .takeWhile(x -> !locationService.samePoint(x, endLocation)).collect(Collectors.toList());

        byte downriver = railwayLineSection.getDownriver();
        double kilometer = 0;
        Location previousLocation = new Location();
        for (int i = 0; i < railwayLineSectionLocations.size(); i++) {
            Location railwayLineSectionLocation = railwayLineSectionLocations.get(i);
            if (i == 0) previousLocation = railwayLineSectionLocation;
            double distance = LocationUtil.getDistance(previousLocation, railwayLineSectionLocation);
            kilometer += distance;
            previousLocation = railwayLineSectionLocation;
            // ?????????????????????????????????????????????
            if (RailwayLineSection.DOWNRIVER == downriver || RailwayLineSection.SINGLE_LINE == downriver) {
                if (i == 0) startKilometerMark.setKilometer(kilometer);
                if (i == railwayLineSectionLocations.size() - 1) endKilometerMark.setKilometer(kilometer);
            }
            if (RailwayLineSection.UPRIVER == downriver) {
                if (i == 0) endKilometerMark.setKilometer(kilometer);
                if (i == railwayLineSectionLocations.size() - 1) startKilometerMark.setKilometer(kilometer);
            }
        }
        kilometerMarkBaseDao.saveOrUpdate(startKilometerMark);
        kilometerMarkBaseDao.saveOrUpdate(endKilometerMark);
        Location startKilometerMarkLocation = new Location(startKilometerMark.getId(), startLocation.getLongitude(),
                startLocation.getLatitude(), 0, Location.KILOMETER_MARKS);
        Location endKilometerMarkLocation = new Location(endKilometerMark.getId(), endLocation.getLongitude(), endLocation.getLatitude(),
                0, Location.KILOMETER_MARKS);

        locationService.saveOrUpdate(startKilometerMarkLocation);
        locationService.saveOrUpdate(endKilometerMarkLocation);

        return new Pair<>(startKilometerMark, endKilometerMark);
    }

    /**
     * ????????????????????????????????????
     *
     * @param startStationId       ?????????
     * @param endStationId         ?????????
     * @param railwayLineSection ????????????
     * @return Pair.firstValue:??????????????????Pair.secondValue:???????????????
     */
    private Pair<KilometerMark, KilometerMark> createStartEndKilometerMark(
            String startStationId, String endStationId, RailwayLineSection railwayLineSection, List<Location> railwayLineSectionLocations
    ) {
        String railwayLineSectionId = railwayLineSection.getId();
        byte downriver = railwayLineSection.getDownriver();
        KilometerMark startKilometerMark = getByRailwaylineSectionIdAndStationId(startStationId, railwayLineSectionId);
        KilometerMark endKilometerMark = getByRailwaylineSectionIdAndStationId(endStationId, railwayLineSectionId);

        if (startKilometerMark != null && endKilometerMark != null)
            return new Pair<>(startKilometerMark, endKilometerMark);

        if (startKilometerMark == null)
            startKilometerMark = new KilometerMark(railwayLineSectionId, startStationId, 0);
        if (endKilometerMark == null)
            endKilometerMark = new KilometerMark(railwayLineSectionId, endStationId, 0);

        double kilometer = startKilometerMark.getKilometer();
        Location previousLocation = new Location();
        for (int i = 0; i < railwayLineSectionLocations.size(); i++) {
            Location railwayLineSectionLocation = railwayLineSectionLocations.get(i);
            if (i == 0) previousLocation = railwayLineSectionLocation;
            double distance = LocationUtil.getDistance(previousLocation, railwayLineSectionLocation);
            kilometer += distance;
            previousLocation = railwayLineSectionLocation;
            // ?????????????????????????????????????????????
            if (RailwayLineSection.DOWNRIVER == downriver || RailwayLineSection.SINGLE_LINE == downriver) {
                if (i == 0) startKilometerMark.setKilometer(kilometer);
                if (i == railwayLineSectionLocations.size() - 1) endKilometerMark.setKilometer(kilometer);
            }
            if (RailwayLineSection.UPRIVER == downriver) {
                if (i == 0) endKilometerMark.setKilometer(kilometer);
                if (i == railwayLineSectionLocations.size() - 1) startKilometerMark.setKilometer(kilometer);
            }
        }
        kilometerMarkBaseDao.saveOrUpdate(startKilometerMark);
        kilometerMarkBaseDao.saveOrUpdate(endKilometerMark);
        Location startLocation = railwayLineSectionLocations.get(0);
        Location endLocation = railwayLineSectionLocations.get(railwayLineSectionLocations.size() - 1);
        Location startKilometerMarkLocation = new Location(startKilometerMark.getId(), startLocation.getLongitude(),
                startLocation.getLatitude(), 0, Location.KILOMETER_MARKS);
        Location endKilometerMarkLocation = new Location(endKilometerMark.getId(), endLocation.getLongitude(), endLocation.getLatitude(),
                0, Location.KILOMETER_MARKS);

        locationService.saveOrUpdate(startKilometerMarkLocation);
        locationService.saveOrUpdate(endKilometerMarkLocation);

        return new Pair<>(startKilometerMark, endKilometerMark);
    }
}
