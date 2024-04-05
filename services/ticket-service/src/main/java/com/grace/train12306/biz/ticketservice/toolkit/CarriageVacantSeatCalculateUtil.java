package com.grace.train12306.biz.ticketservice.toolkit;

import cn.hutool.core.lang.Pair;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * 座位统计工具类
 */
public final class CarriageVacantSeatCalculateUtil {

    /**
     * 座位统计方法
     *
     * @param actualSeats 座位选座情况二位数组
     * @param n           列数
     * @param m           行数
     * @return 空余座位集合小根堆
     */
    public static PriorityQueue<List<Pair<Integer, Integer>>> buildCarriageVacantSeatList(int[][] actualSeats, int n, int m) {
        PriorityQueue<List<Pair<Integer, Integer>>> vacantSeatQueue = new PriorityQueue<>(Comparator.comparingInt(List::size));
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                if (actualSeats[i][j] == 0) {
                    List<Pair<Integer, Integer>> res = new ArrayList<>();
                    int k = j;
                    for (; k < m; k++) {
                        if (actualSeats[i][k] == 1) break;
                        res.add(new Pair<>(i, k));
                    }
                    j = k;
                    vacantSeatQueue.add(res);
                }
            }
        }
        return vacantSeatQueue;
    }

    /**
     * 空余座位统计方法
     *
     * @param actualSeats 座位状态数组
     * @param n
     * @param m
     * @return 空余座位集合
     */
    public static List<Pair<Integer, Integer>> buildCarriageVacantSeatList2(int[][] actualSeats, int n, int m) {
        List<Pair<Integer, Integer>> vacantSeatList = new ArrayList<>(16);
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                if (actualSeats[i][j] == 0) {
                    vacantSeatList.add(new Pair<>(i, j));
                }
            }
        }
        return vacantSeatList;
    }
}
