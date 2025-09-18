package model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Objects;

public class Match {
    public int matchId;
    public MatchType type;     // ONE_VS_ONE / MULTIPLAYER
    public String creatorId;
    public Timestamp startTime;
    public Timestamp endTime;

    public enum MatchType { ONE_VS_ONE, MULTIPLAYER }

    public Match() {}

    public Match(int matchId, MatchType type, String creatorId, Timestamp startTime, Timestamp endTime) {
        this.matchId = matchId;
        this.type = type;
        this.creatorId = creatorId;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public static Match fromResultSet(ResultSet rs) throws SQLException {
        String t = rs.getString("type");
        MatchType mt = t == null ? null : MatchType.valueOf(t);
        return new Match(
                rs.getInt("match_id"),
                mt,
                rs.getString("creator_id"),
                rs.getTimestamp("start_time"),
                rs.getTimestamp("end_time")
        );
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Match)) return false;
        Match match = (Match) o;
        return matchId == match.matchId;
    }
    @Override public int hashCode() { return Objects.hash(matchId); }

    @Override public String toString() {
        return "Match{" +
                "matchId=" + matchId +
                ", type=" + type +
                ", creatorId='" + creatorId + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                '}';
    }
}
