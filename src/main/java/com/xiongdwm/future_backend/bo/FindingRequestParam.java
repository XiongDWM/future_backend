package com.xiongdwm.future_backend.bo;

public class FindingRequestParam {
    private Boolean man; // 男单true 女单false null不限
    private String gameType; // 游戏类型
    private String rank; // 段位
    public Boolean isMan() {
        return man;
    }
    public void setMan(Boolean man) {
        this.man = man;
    }
    public String getGameType() {
        return gameType;
    }
    public void setGameType(String gameType) {
        this.gameType = gameType;
    }
    public String getRank() {
        return rank;
    }
    public void setRank(String rank) {
        this.rank = rank;
    }
    
}
