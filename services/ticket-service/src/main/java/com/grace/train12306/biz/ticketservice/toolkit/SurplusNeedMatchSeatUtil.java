package com.grace.train12306.biz.ticketservice.toolkit;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;

/**
 * 匹配剩余的座位工具类
 */
public final class SurplusNeedMatchSeatUtil {

    /**
     * 匹配指定数量的空余座位方法
     *
     * @param chooseSeatSize  选择座位数量
     * @param vacantSeatQueue 空余座位集合
     * @return 获取选择座位数量的空余座位集合 (获取数量可能小于选择座位数量)
     */
    public static List<Pair<Integer, Integer>> getSurplusNeedMatchSeat(int chooseSeatSize, PriorityQueue<List<Pair<Integer, Integer>>> vacantSeatQueue) {
        Optional<List<Pair<Integer, Integer>>> optionalList = vacantSeatQueue.parallelStream().filter(each -> each.size() >= chooseSeatSize).findFirst();
        if (optionalList.isPresent()) {
            return optionalList.get().subList(0, chooseSeatSize);
        }
        List<Pair<Integer, Integer>> result = new ArrayList<>(chooseSeatSize);
        while (CollUtil.isNotEmpty(vacantSeatQueue)) {
            List<Pair<Integer, Integer>> pairList = vacantSeatQueue.poll();
            if (result.size() + pairList.size() < chooseSeatSize) {
                result.addAll(pairList);
            }
            else if (result.size() + pairList.size() >= chooseSeatSize) {
                int needPairListLen = pairList.size() - (result.size() + pairList.size() - chooseSeatSize);
                result.addAll(pairList.subList(0, needPairListLen));
                if (result.size() == chooseSeatSize) {
                    break;
                }
            }
        }
        return result;
    }
}
