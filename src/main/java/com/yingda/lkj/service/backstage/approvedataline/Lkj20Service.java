package com.yingda.lkj.service.backstage.approvedataline;

import com.yingda.lkj.beans.entity.backstage.dataversion.DataApproveFlow;
import com.yingda.lkj.beans.entity.backstage.dataversion.DataVersion;
import com.yingda.lkj.beans.entity.backstage.lkj.lkjextends.Lkj16;
import com.yingda.lkj.beans.entity.backstage.lkj.lkjextends.Lkj18;
import com.yingda.lkj.beans.entity.backstage.lkj.lkjextends.Lkj20;

import java.util.List;

/**
 * @author hood  2020/5/29
 */
public interface Lkj20Service {

    /**
     * 生成待审批的lkjDataLine
     */
    List<Lkj20> createLkjDataLine(DataApproveFlow dataApproveFlow, List<Lkj20> rawLkjDataLines);

    /**
     * 修改dataApproveFlow下的lkj数据为未通过
     */
    void refuseLkjDataLines(DataApproveFlow dataApproveFlow);

    /**
     * 提交审批流下的数据为已完成
     */
    void completeLkjDataLine(DataApproveFlow dataApproveFlow);

    /**
     * 更新版本后，对数据打上版本信息
     */
    void setVersionData(DataVersion dataVersion, List<Lkj20> lkjDataLines);
}
