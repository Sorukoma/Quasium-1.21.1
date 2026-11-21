package net.sorukoma.quasium.screen.program;

public record Part(Kind kind, int x, int y, int width, String text, int corner) {
    public static final int HEIGHT = 12;

    public enum Kind {
        TEXT, PILL_TEXT, SOCKET, FILTER, NUMBER, CHECK;

        boolean clickable() {
            return this != TEXT && this != PILL_TEXT;
        }
    }

    public boolean contains(int pointX, int pointY) {
        return pointX >= x && pointX < x + width && pointY >= y && pointY < y + HEIGHT;
    }
}
