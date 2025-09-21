package server;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Quản lý các mã trận đấu (game codes) để join vào phòng
 */
public class GameCodeManager {
    // Map lưu trữ: matchId -> gameCode
    private static final Map<Integer, String> GAME_CODES = new ConcurrentHashMap<>();

    // Sinh code ngẫu nhiên với độ dài cho trước
    private static String generateSequence(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random rand = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(rand.nextInt(chars.length())));
        }
        return sb.toString();
    }

    // Lấy độ dài code theo round
    private static int getLengthByRound(int round) {
        if (round == 1) return 4;
        else if (round == 2) return 5;
        else if (round >= 3 && round <= 4) return 6;
        else if (round >= 5 && round <= 7) return 7;
        else if (round >= 8 && round <= 10) return 8;
        else if (round >= 11 && round <= 13) return 9;
        else if (round >= 14 && round <= 15) return 10;
        else if (round >= 16 && round <= 17) return 11;
        else if (round >= 18 && round <= 19) return 12;
        else if (round >= 20 && round <= 21) return 13;
        else if (round >= 22 && round <= 24) return 14;
        return 4; // mặc định
    }

    /**
     * Tính điểm giống nhau giữa input và code của match
     * - Điểm = số ký tự giống từ đầu cho đến khi gặp ký tự khác
     */
    public static int calculateSimilarityScore(String input, int matchId) {
        if (input == null || matchId == 0) {
            return 0;
        }
        String code = GAME_CODES.get(matchId);
        if (code == null) {
            return 0;
        }

        int score = 0;
        int minLength = Math.min(input.length(), code.length());

        for (int i = 0; i < minLength; i++) {
            if (input.charAt(i) == code.charAt(i)) {
                score++;
            } else {
                break;
            }
        }
        return score;
    }

    /**
     * Tạo mã cho trận đấu mới theo round
     */
    public static String createGameCode(Integer matchId, int round) {
        int length = getLengthByRound(round);

        String gameCode;
        do {
            gameCode = generateSequence(length);
        } while (GAME_CODES.containsValue(gameCode)); // Đảm bảo không trùng

        GAME_CODES.put(matchId, gameCode);
        return gameCode;
    }

    /**
     * Lấy code theo matchId
     */
    public static String getCodeByMatchId(Integer matchId) {
        return GAME_CODES.get(matchId);
    }

    /**
     * Xóa code khi trận đấu kết thúc
     */
    public static void removeGameCode(Integer matchId) {
        GAME_CODES.remove(matchId);
    }

    /**
     * Kiểm tra matchId có tồn tại không
     */
    public static boolean containsMatchId(Integer matchId) {
        return GAME_CODES.containsKey(matchId);
    }

    /**
     * Cập nhật code cho match
     */
    public static void updateGameCode(Integer matchId, String code) {
        if (GAME_CODES.containsKey(matchId)) {
            GAME_CODES.put(matchId, code);
        }
    }

    /**
     * Lấy toàn bộ map (debug)
     */
    public static Map<Integer, String> getAllCodes() {
        return GAME_CODES;
    }
}
