import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Single-month calendar with a horizontal Cirlox spiral binding at the top-center.
 * Navigate with ◀ Prev / Today / Next ▶.
 */
public class ThreeMonthCalendar extends Application {

    // ── Colour palette ───────────────────────────────────────────────────────
    private static final Color BG_CREAM      = Color.rgb(245, 240, 230);
    private static final Color PAPER_WHITE   = Color.rgb(255, 252, 245);
    private static final Color BINDING_METAL = Color.rgb(180, 185, 195);
    private static final Color BINDING_DARK  = Color.rgb(90,  95, 110);
    private static final Color BINDING_LIGHT = Color.rgb(220, 225, 235);
    private static final Color RING_SHADOW   = Color.rgb(60,  65,  80, 0.45);
    private static final Color HEADER_INK    = Color.rgb(35,  35,  50);
    private static final Color WEEKEND_BLUE  = Color.rgb(60,  90, 160);
    private static final Color CELL_RULE     = Color.rgb(200, 195, 185);
    private static final Color TODAY_FILL    = Color.rgb(190, 50,  40, 0.15);
    private static final Color TODAY_RING    = Color.rgb(190, 50,  40);
    private static final Color HOLES_AREA    = Color.rgb(225, 220, 210);

    // ── Layout constants ─────────────────────────────────────────────────────
    private static final double RING_STRIP_H = 54;   // height of the top binding strip
    private static final double RING_RADIUS  = 11;   // outer radius of each Cirlox ring
    private static final double RING_SPACING = 34;   // horizontal ring centre-to-centre
    private static final double CELL_W       = 96;
    private static final double CELL_H       = 80;
    private static final double PAGE_W       = 7 * CELL_W;   // 672 px
    private static final double OUTER_PAD    = 28;

    private YearMonth currentMonth;
    private VBox      pageContainer;   // rebuilt on every navigation
    private Stage     primaryStage;

    // ════════════════════════════════════════════════════════════════════════
    @Override
    public void start(Stage stage) {
        primaryStage  = stage;
        currentMonth  = YearMonth.now();

        VBox root = new VBox(0);
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(OUTER_PAD, OUTER_PAD, OUTER_PAD + 8, OUTER_PAD));
        root.setBackground(new Background(
            new BackgroundFill(BG_CREAM, CornerRadii.EMPTY, Insets.EMPTY)));

        // Navigation bar
        root.getChildren().add(buildNavBar());

        // Notepad (binding + month page, rebuilt on navigation)
        pageContainer = new VBox(0);
        pageContainer.setAlignment(Pos.TOP_CENTER);
        rebuildPage();
        root.getChildren().add(pageContainer);

        Scene scene = new Scene(root);
        scene.setFill(BG_CREAM);
        stage.setTitle("Cirlox · Monthly Calendar");
        stage.setScene(scene);
        stage.sizeToScene();
        stage.setResizable(false);
        stage.show();
    }

    // ── Navigation bar ───────────────────────────────────────────────────────
    private HBox buildNavBar() {
        HBox bar = new HBox(14);
        bar.setAlignment(Pos.CENTER);
        bar.setPadding(new Insets(0, 0, 14, 0));

        Button prev  = navButton("◀  Prev");
        Button today = navButton("Today");
        Button next  = navButton("Next  ▶");

        prev .setOnAction(e -> { currentMonth = currentMonth.minusMonths(1); rebuildPage(); });
        today.setOnAction(e -> { currentMonth = YearMonth.now();             rebuildPage(); });
        next .setOnAction(e -> { currentMonth = currentMonth.plusMonths(1);  rebuildPage(); });

        bar.getChildren().addAll(prev, today, next);
        return bar;
    }

    private Button navButton(String label) {
        String base =
            "-fx-background-color: " + toHex(PAPER_WHITE)   + ";" +
            "-fx-border-color: "     + toHex(BINDING_METAL) + ";" +
            "-fx-border-radius: 4px; -fx-background-radius: 4px;" +
            "-fx-padding: 5 16 5 16; -fx-cursor: hand;";
        String hover =
            "-fx-background-color: " + toHex(BINDING_LIGHT) + ";" +
            "-fx-border-color: "     + toHex(BINDING_DARK)  + ";" +
            "-fx-border-radius: 4px; -fx-background-radius: 4px;" +
            "-fx-padding: 5 16 5 16; -fx-cursor: hand;";

        Button btn = new Button(label);
        btn.setFont(Font.font("Georgia", FontWeight.BOLD, 13));
        btn.setTextFill(HEADER_INK);
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited (e -> btn.setStyle(base));
        return btn;
    }

    // ── Full page rebuild ────────────────────────────────────────────────────
    private void rebuildPage() {
        pageContainer.getChildren().clear();

        // 1. Horizontal Cirlox binding strip (top-center of the notepad)
        pageContainer.getChildren().add(buildTopBindingStrip());

        // 2. Dark month/year header banner
        pageContainer.getChildren().add(buildMonthHeader());

        // 3. Day-of-week label row
        pageContainer.getChildren().add(buildDowHeader());

        // 4. Week rows (up to 6)
        pageContainer.getChildren().addAll(buildWeekRows());

        primaryStage.sizeToScene();
    }

    // ── Horizontal Cirlox binding strip ─────────────────────────────────────
    private Canvas buildTopBindingStrip() {
        Canvas canvas = new Canvas(PAGE_W, RING_STRIP_H);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Strip background
        gc.setFill(HOLES_AREA);
        gc.fillRect(0, 0, PAGE_W, RING_STRIP_H);

        // Subtle top-down shading gradient
        LinearGradient shade = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0.0, Color.rgb(80, 80, 100, 0.14)),
            new Stop(1.0, Color.TRANSPARENT));
        gc.setFill(shade);
        gc.fillRect(0, 0, PAGE_W, RING_STRIP_H);

        // Bottom edge line separating strip from page
        gc.setStroke(Color.rgb(150, 145, 135, 0.9));
        gc.setLineWidth(1.2);
        gc.strokeLine(0, RING_STRIP_H - 1, PAGE_W, RING_STRIP_H - 1);

        // Place rings evenly across the full width
        double margin       = RING_SPACING * 0.75;
        double usableWidth  = PAGE_W - 2 * margin;
        int    ringCount    = Math.max(2, (int)(usableWidth / RING_SPACING) + 1);
        double actualGap    = usableWidth / (ringCount - 1);
        double cy           = RING_STRIP_H / 2 + 1;

        for (int i = 0; i < ringCount; i++) {
            double cx = margin + i * actualGap;
            drawCirloxRingH(gc, cx, cy);
        }

        return canvas;
    }

    /**
     * Draws a single Cirlox ring centred at (cx, cy).
     * The C-opening faces DOWNWARD so pages appear to slide in from below — 
     * matching the look of a top-bound Cirlox notepad.
     */
    private void drawCirloxRingH(GraphicsContext gc, double cx, double cy) {
        double r  = RING_RADIUS;
        double rI = r * 0.53;

        // --- Drop shadow ---
        gc.setFill(RING_SHADOW);
        gc.fillOval(cx - r + 2, cy - r + 3, r * 2, r * 2);

        // --- Metallic body (left-to-right gradient for horizontal lighting) ---
        LinearGradient metal = new LinearGradient(cx - r, cy, cx + r, cy, false, CycleMethod.NO_CYCLE,
            new Stop(0.00, BINDING_DARK),
            new Stop(0.30, BINDING_LIGHT),
            new Stop(0.62, BINDING_METAL),
            new Stop(1.00, BINDING_DARK));
        gc.setFill(metal);
        gc.fillOval(cx - r, cy - r, r * 2, r * 2);

        // --- Punched hole ---
        gc.setFill(HOLES_AREA);
        gc.fillOval(cx - rI, cy - rI, rI * 2, rI * 2);

        // --- C-opening gap facing downward (centred on 270°) ---
        double gapDeg    = 62.0;
        double startDeg  = 270.0 - gapDeg / 2.0;   // standard math angle (CCW, 0 = right)

        gc.save();
        gc.setFill(HOLES_AREA);
        gc.beginPath();
        gc.moveTo(cx, cy);

        int steps = 16;
        for (int s = 0; s <= steps; s++) {
            double angleDeg = startDeg + gapDeg * s / steps;
            double angleRad = Math.toRadians(angleDeg);
            // JavaFX Y-axis is flipped, so sin is negated
            gc.lineTo(cx + (r + 1.5) * Math.cos(angleRad),
                      cy - (r + 1.5) * Math.sin(angleRad));
        }
        gc.closePath();
        gc.fill();
        gc.restore();

        // --- Outer stroke ---
        gc.setStroke(BINDING_DARK);
        gc.setLineWidth(0.9);
        gc.strokeOval(cx - r, cy - r, r * 2, r * 2);

        // --- Inner hole stroke ---
        gc.setStroke(Color.rgb(125, 130, 145, 0.75));
        gc.setLineWidth(0.6);
        gc.strokeOval(cx - rI, cy - rI, rI * 2, rI * 2);

        // --- Specular highlight (upper-left arc) ---
        gc.setStroke(Color.rgb(255, 255, 255, 0.58));
        gc.setLineWidth(1.4);
        gc.strokeArc(cx - r + 2.5, cy - r + 2.5, (r - 2.5) * 2, (r - 2.5) * 2, 105, 68, ArcType.OPEN);
    }

    // ── Month / year header banner ───────────────────────────────────────────
    private HBox buildMonthHeader() {
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER);
        header.setMinWidth(PAGE_W);
        header.setMaxWidth(PAGE_W);
        header.setMinHeight(50);
        header.setBackground(new Background(new BackgroundFill(
            Color.rgb(35, 35, 50), CornerRadii.EMPTY, Insets.EMPTY)));

        String month = currentMonth.getMonth()
            .getDisplayName(TextStyle.FULL, Locale.getDefault()).toUpperCase();
        String year  = String.valueOf(currentMonth.getYear());

        Text title = new Text(month + "   " + year);
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 22));
        title.setFill(PAPER_WHITE);
        title.setTextAlignment(TextAlignment.CENTER);

        header.getChildren().add(title);
        return header;
    }

    // ── Day-of-week label row ────────────────────────────────────────────────
    private HBox buildDowHeader() {
        String[] days = { "SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT" };
        HBox row = new HBox(0);
        row.setMinWidth(PAGE_W);

        for (int i = 0; i < 7; i++) {
            boolean weekend = (i == 0 || i == 6);
            StackPane cell = new StackPane();
            cell.setMinWidth(CELL_W);
            cell.setMaxWidth(CELL_W);
            cell.setMinHeight(30);
            cell.setBackground(new Background(new BackgroundFill(
                weekend ? Color.rgb(228, 222, 212) : Color.rgb(238, 234, 225),
                CornerRadii.EMPTY, Insets.EMPTY)));
            cell.setBorder(new Border(new BorderStroke(
                CELL_RULE, BorderStrokeStyle.SOLID, CornerRadii.EMPTY,
                new BorderWidths(0, i < 6 ? 1 : 0, 1, 0))));

            Text t = new Text(days[i]);
            t.setFont(Font.font("Georgia", FontWeight.BOLD, 11));
            t.setFill(weekend ? WEEKEND_BLUE : Color.rgb(100, 95, 85));
            cell.getChildren().add(t);
            row.getChildren().add(cell);
        }
        return row;
    }

    // ── Week cell rows ───────────────────────────────────────────────────────
    private List<HBox> buildWeekRows() {
        List<HBox> rowList = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate first = currentMonth.atDay(1);

        // Find the Sunday on or before the 1st of the month
        int dow      = first.getDayOfWeek().getValue();   // Mon=1 … Sun=7
        int backDays = (dow == 7) ? 0 : dow;
        LocalDate weekStart = first.minusDays(backDays);

        for (int week = 0; week < 6; week++) {
            LocalDate sunday   = weekStart.plusWeeks(week);
            LocalDate saturday = sunday.plusDays(6);

            // Skip entirely if neither end of the week touches this month
            if (sunday  .getMonth() != currentMonth.getMonth() &&
                saturday.getMonth() != currentMonth.getMonth()) break;

            HBox row = new HBox(0);
            for (int d = 0; d < 7; d++) {
                LocalDate date     = sunday.plusDays(d);
                boolean   inMonth  = date.getMonth() == currentMonth.getMonth();
                boolean   isToday  = date.equals(today);
                boolean   weekend  = (d == 0 || d == 6);

                StackPane cell = new StackPane();
                cell.setMinSize(CELL_W, CELL_H);
                cell.setMaxSize(CELL_W, CELL_H);
                cell.setAlignment(Pos.TOP_RIGHT);
                cell.setPadding(new Insets(5, 7, 4, 4));

                // Cell background
                Color bg;
                if      (!inMonth) bg = Color.rgb(233, 229, 218);
                else if (isToday)  bg = TODAY_FILL;
                else if (weekend)  bg = Color.rgb(250, 248, 241);
                else               bg = PAPER_WHITE;

                cell.setBackground(new Background(
                    new BackgroundFill(bg, CornerRadii.EMPTY, Insets.EMPTY)));
                cell.setBorder(new Border(new BorderStroke(
                    CELL_RULE, BorderStrokeStyle.SOLID, CornerRadii.EMPTY,
                    new BorderWidths(0, d < 6 ? 1 : 0, 1, 0))));

                // Notebook ruled lines
                Canvas lines = new Canvas(CELL_W, CELL_H);
                GraphicsContext lg = lines.getGraphicsContext2D();
                lg.setStroke(Color.rgb(210, 205, 192, 0.55));
                lg.setLineWidth(0.5);
                for (double ly = 26; ly < CELL_H - 5; ly += 15) {
                    lg.strokeLine(6, ly, CELL_W - 6, ly);
                }
                cell.getChildren().add(lines);

                // Date number
                Text num = new Text(String.valueOf(date.getDayOfMonth()));
                num.setFont(Font.font("Georgia", FontWeight.BOLD, 14));

                Color numColor;
                if      (!inMonth) numColor = Color.rgb(175, 170, 158);
                else if (weekend)  numColor = WEEKEND_BLUE;
                else               numColor = HEADER_INK;
                num.setFill(numColor);

                if (isToday) {
                    StackPane badge = new StackPane();
                    badge.setAlignment(Pos.CENTER);
                    badge.setMinSize(24, 24);
                    badge.setMaxSize(24, 24);
                    Circle ring = new Circle(12);
                    ring.setFill(Color.TRANSPARENT);
                    ring.setStroke(TODAY_RING);
                    ring.setStrokeWidth(2.0);
                    num.setFill(TODAY_RING);
                    badge.getChildren().addAll(ring, num);
                    cell.getChildren().add(badge);
                    StackPane.setAlignment(badge, Pos.TOP_RIGHT);
                } else {
                    cell.getChildren().add(num);
                    StackPane.setAlignment(num, Pos.TOP_RIGHT);
                }

                row.getChildren().add(cell);
            }
            rowList.add(row);
        }
        return rowList;
    }

    // ── Utility ──────────────────────────────────────────────────────────────
    private String toHex(Color c) {
        return String.format("#%02X%02X%02X",
            (int)(c.getRed()   * 255),
            (int)(c.getGreen() * 255),
            (int)(c.getBlue()  * 255));
    }

    public static void main(String[] args) {
        launch(args);
    }
}