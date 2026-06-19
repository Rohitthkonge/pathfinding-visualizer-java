import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

/**
 * Pathfinding Visualizer
 * - Click a cell to place START (1st click), then END (2nd click), then WALLS (rest).
 * - Press "Run Dijkstra" or "Run A*" to animate the search.
 * - "Clear Path" keeps walls but resets the search. "Reset Grid" clears everything.
 */
public class PathfindingVisualizer extends JFrame {

    static final int ROWS = 20;
    static final int COLS = 30;
    static final int CELL_SIZE = 25;

    static final Color COLOR_EMPTY = Color.WHITE;
    static final Color COLOR_WALL = new Color(40, 44, 52);
    static final Color COLOR_START = new Color(46, 204, 113);
    static final Color COLOR_END = new Color(231, 76, 60);
    static final Color COLOR_VISITED = new Color(174, 214, 241);
    static final Color COLOR_FRONTIER = new Color(241, 196, 15);
    static final Color COLOR_PATH = new Color(155, 89, 182);
    static final Color COLOR_GRID = new Color(220, 220, 220);

    Cell[][] grid = new Cell[ROWS][COLS];
    Cell start = null;
    Cell end = null;
    int clickStage = 0; // 0 = placing start, 1 = placing end, 2 = placing/removing walls

    GridPanel gridPanel;
    JLabel statusLabel;
    Timer animationTimer;

    public PathfindingVisualizer() {
        setTitle("Pathfinding Visualizer - Dijkstra & A*");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        for (int r = 0; r < ROWS; r++)
            for (int c = 0; c < COLS; c++)
                grid[r][c] = new Cell(r, c);

        gridPanel = new GridPanel();
        gridPanel.setPreferredSize(new Dimension(COLS * CELL_SIZE, ROWS * CELL_SIZE));
        gridPanel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                handleClick(e.getX() / CELL_SIZE, e.getY() / CELL_SIZE);
            }
        });
        gridPanel.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                int col = e.getX() / CELL_SIZE;
                int row = e.getY() / CELL_SIZE;
                if (clickStage == 2 && inBounds(row, col)) {
                    Cell c = grid[row][col];
                    if (c != start && c != end) {
                        c.isWall = true;
                        gridPanel.repaint();
                    }
                }
            }
        });

        JPanel controls = new JPanel();
        JButton dijkstraBtn = new JButton("Run Dijkstra");
        JButton astarBtn = new JButton("Run A*");
        JButton clearPathBtn = new JButton("Clear Path");
        JButton resetBtn = new JButton("Reset Grid");

        dijkstraBtn.addActionListener(e -> runSearch(false));
        astarBtn.addActionListener(e -> runSearch(true));
        clearPathBtn.addActionListener(e -> clearPathOnly());
        resetBtn.addActionListener(e -> resetGrid());

        controls.add(dijkstraBtn);
        controls.add(astarBtn);
        controls.add(clearPathBtn);
        controls.add(resetBtn);

        statusLabel = new JLabel("Click a cell to place START");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        add(controls, BorderLayout.NORTH);
        add(gridPanel, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    boolean inBounds(int r, int c) {
        return r >= 0 && r < ROWS && c >= 0 && c < COLS;
    }

    void handleClick(int col, int row) {
        if (!inBounds(row, col)) return;
        Cell clicked = grid[row][col];

        if (clickStage == 0) {
            start = clicked;
            clicked.isWall = false;
            clickStage = 1;
            statusLabel.setText("Now click a cell to place END");
        } else if (clickStage == 1) {
            if (clicked == start) return;
            end = clicked;
            clicked.isWall = false;
            clickStage = 2;
            statusLabel.setText("Click/drag to add walls, then press Run Dijkstra or Run A*");
        } else {
            if (clicked == start || clicked == end) return;
            clicked.isWall = !clicked.isWall;
        }
        gridPanel.repaint();
    }

    void resetGrid() {
        if (animationTimer != null) animationTimer.stop();
        for (int r = 0; r < ROWS; r++)
            for (int c = 0; c < COLS; c++)
                grid[r][c] = new Cell(r, c);
        start = null;
        end = null;
        clickStage = 0;
        statusLabel.setText("Click a cell to place START");
        gridPanel.repaint();
    }

    void clearPathOnly() {
        if (animationTimer != null) animationTimer.stop();
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                Cell cell = grid[r][c];
                cell.visited = false;
                cell.inPath = false;
                cell.distance = Integer.MAX_VALUE;
                cell.previous = null;
            }
        }
        statusLabel.setText("Path cleared. Walls kept. Run a search again.");
        gridPanel.repaint();
    }

    // ---------- Search algorithms ----------

    void runSearch(boolean useAStar) {
        if (start == null || end == null) {
            statusLabel.setText("Please place both START and END first.");
            return;
        }
        clearPathOnly();
        if (animationTimer != null) animationTimer.stop();

        List<Cell> visitedOrder = new ArrayList<>();
        boolean found = useAStar ? aStar(visitedOrder) : dijkstra(visitedOrder);

        statusLabel.setText((useAStar ? "A*" : "Dijkstra") + " running...");

        final int[] i = {0};
        animationTimer = new Timer(15, e -> {
            if (i[0] < visitedOrder.size()) {
                Cell c = visitedOrder.get(i[0]);
                if (c != start && c != end) c.visited = true;
                i[0]++;
                gridPanel.repaint();
            } else {
                animationTimer.stop();
                if (found) {
                    animatePath(useAStar);
                } else {
                    statusLabel.setText("No path found - end is blocked off.");
                }
            }
        });
        animationTimer.start();
    }

    void animatePath(boolean useAStar) {
        List<Cell> path = new ArrayList<>();
        Cell cur = end;
        while (cur != null) {
            path.add(cur);
            cur = cur.previous;
        }
        Collections.reverse(path);

        final int[] i = {0};
        Timer pathTimer = new Timer(25, e -> {
            if (i[0] < path.size()) {
                Cell c = path.get(i[0]);
                if (c != start && c != end) c.inPath = true;
                i[0]++;
                gridPanel.repaint();
            } else {
                ((Timer) e.getSource()).stop();
                statusLabel.setText((useAStar ? "A*" : "Dijkstra") + " found a path of length " + (path.size() - 1) + " cells.");
            }
        });
        pathTimer.start();
    }

    List<Cell> neighbors(Cell c) {
        List<Cell> result = new ArrayList<>();
        int[][] dirs = {{-1,0},{1,0},{0,-1},{0,1}};
        for (int[] d : dirs) {
            int nr = c.row + d[0];
            int nc = c.col + d[1];
            if (inBounds(nr, nc) && !grid[nr][nc].isWall) {
                result.add(grid[nr][nc]);
            }
        }
        return result;
    }

    boolean dijkstra(List<Cell> visitedOrder) {
        for (int r = 0; r < ROWS; r++)
            for (int c = 0; c < COLS; c++)
                grid[r][c].distance = Integer.MAX_VALUE;

        start.distance = 0;
        PriorityQueue<Cell> pq = new PriorityQueue<>(Comparator.comparingInt(c -> c.distance));
        pq.add(start);
        Set<Cell> done = new HashSet<>();

        while (!pq.isEmpty()) {
            Cell current = pq.poll();
            if (done.contains(current)) continue;
            done.add(current);
            visitedOrder.add(current);

            if (current == end) return true;

            for (Cell n : neighbors(current)) {
                int newDist = current.distance + 1;
                if (newDist < n.distance) {
                    n.distance = newDist;
                    n.previous = current;
                    pq.add(n);
                }
            }
        }
        return false;
    }

    boolean aStar(List<Cell> visitedOrder) {
        for (int r = 0; r < ROWS; r++)
            for (int c = 0; c < COLS; c++)
                grid[r][c].distance = Integer.MAX_VALUE;

        start.distance = 0;
        Map<Cell, Integer> fScore = new HashMap<>();
        fScore.put(start, heuristic(start, end));

        PriorityQueue<Cell> pq = new PriorityQueue<>(Comparator.comparingInt(c -> fScore.getOrDefault(c, Integer.MAX_VALUE)));
        pq.add(start);
        Set<Cell> done = new HashSet<>();

        while (!pq.isEmpty()) {
            Cell current = pq.poll();
            if (done.contains(current)) continue;
            done.add(current);
            visitedOrder.add(current);

            if (current == end) return true;

            for (Cell n : neighbors(current)) {
                int tentativeG = current.distance + 1;
                if (tentativeG < n.distance) {
                    n.distance = tentativeG;
                    n.previous = current;
                    fScore.put(n, tentativeG + heuristic(n, end));
                    pq.add(n);
                }
            }
        }
        return false;
    }

    int heuristic(Cell a, Cell b) {
        // Manhattan distance
        return Math.abs(a.row - b.row) + Math.abs(a.col - b.col);
    }

    // ---------- Cell model ----------

    class Cell {
        int row, col;
        boolean isWall = false;
        boolean visited = false;
        boolean inPath = false;
        int distance = Integer.MAX_VALUE;
        Cell previous = null;

        Cell(int row, int col) {
            this.row = row;
            this.col = col;
        }
    }

    // ---------- Drawing ----------

    class GridPanel extends JPanel {
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            for (int r = 0; r < ROWS; r++) {
                for (int c = 0; c < COLS; c++) {
                    Cell cell = grid[r][c];
                    Color color = COLOR_EMPTY;

                    if (cell.isWall) color = COLOR_WALL;
                    else if (cell == start) color = COLOR_START;
                    else if (cell == end) color = COLOR_END;
                    else if (cell.inPath) color = COLOR_PATH;
                    else if (cell.visited) color = COLOR_VISITED;

                    g.setColor(color);
                    g.fillRect(c * CELL_SIZE, r * CELL_SIZE, CELL_SIZE, CELL_SIZE);

                    g.setColor(COLOR_GRID);
                    g.drawRect(c * CELL_SIZE, r * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                }
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(PathfindingVisualizer::new);
    }
}
