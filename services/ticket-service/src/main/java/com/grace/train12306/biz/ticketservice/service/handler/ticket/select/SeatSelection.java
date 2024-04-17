package com.grace.train12306.biz.ticketservice.service.handler.ticket.select;

/**
 * 座位选择器
 */
public class SeatSelection {

    /**
     * 找到一排中连续相邻的座位
     *
     * @param numSeats   需要的座位数量
     * @param seatLayout 座位情况
     * @return 分配结果
     */
    public static int[][] adjacent(int numSeats, int[][] seatLayout) {
        int numRows = seatLayout.length;
        int numCols = seatLayout[0].length;
        int[][] resultSeats = new int[numSeats][2]; // 存储结果的数组，初始化大小为所需座位数
        boolean found = false; // 标记是否找到足够的空座位
        for (int row = 0; row < numRows && !found; row++) {
            for (int col = 0; col < numCols; col++) {
                // 检查当前座位是否为空
                if (seatLayout[row][col] == 0) {
                    int count = 1; // 记录连续空座位数，从当前空座位开始
                    // 检查后续座位
                    while (col + count < numCols && seatLayout[row][col + count] == 0 && count < numSeats) {
                        count++;
                    }
                    // 检查是否找到足够的连续空座位
                    if (count == numSeats) {
                        // 记录空座位的位置
                        for (int i = 0; i < numSeats; i++) {
                            resultSeats[i][0] = row + 1; // 转换为1-based索引
                            resultSeats[i][1] = col + i + 1; // 转换为1-based索引
                        }
                        found = true; // 标记已找到
                        break;
                    }
                    col += count - 1; // 移动到最后一个检查过的空座位
                }
            }
        }
        // 如果未找到足够的连续空座位，则返回null
        return found ? resultSeats : null;
    }

    /**
     * 找到不相邻的座位
     *
     * @param numSeats   需要的座位数量
     * @param seatLayout 座位情况
     * @return 分配结果
     */
    public static int[][] nonAdjacent(int numSeats, int[][] seatLayout) {
        int numRows = seatLayout.length;
        int numCols = seatLayout[0].length;
        int[][] actualSeat = new int[numSeats][2];
        int count = 0;
        outerLoop:
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numCols; j++) {
                if (seatLayout[i][j] == 0) {
                    actualSeat[count][0] = i + 1;
                    actualSeat[count][1] = j + 1;
                    count++;
                    if (count == numSeats) {
                        break outerLoop;
                    }
                }
            }
        }
        return count < numSeats ? null : actualSeat;
    }
}
