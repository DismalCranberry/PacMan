import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.Timer;
import java.util.*;
import java.util.List;

public class PacmanGame extends JFrame implements ActionListener {
    private GamePanel gamePanel;     // The panel that runs the game
    private final JButton restartButton;  // Button to restart the game

    public PacmanGame() {
        super("Pac-Man: Inaccessible Blocks Filled");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Create the game panel (e.g., 15x15 grid)
        gamePanel = new GamePanel(15);
        add(gamePanel, BorderLayout.CENTER);

        // Create the "Restart" button at the bottom.
        restartButton = new JButton("Restart");
        restartButton.addActionListener(this);
        add(restartButton, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // Restart button action: remove old panel, add new one and request focus.
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == restartButton) {
            remove(gamePanel);
            gamePanel = new GamePanel(15);
            add(gamePanel, BorderLayout.CENTER);
            gamePanel.requestFocusInWindow();
            revalidate();
            repaint();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(PacmanGame::new);
    }

    // ------------------------------------------------------------------------
    // Inner class containing gameplay, rendering, and logic.
    // ------------------------------------------------------------------------
    private class GamePanel extends JPanel implements ActionListener, KeyListener {
        private final int BLOCK_SIZE = 24;  // size of one cell (in pixels)
        private final int NUM_BLOCKS;            // grid dimensions (NUM_BLOCKS x NUM_BLOCKS)
        private final int SCREEN_SIZE;          // total screen size (in pixels)

        // Maze representation: 0 = empty, 1 = wall, 2 = pellet
        private int[][] maze;

        // Pac-Man state
        private int pacmanX, pacmanY;       // Pac-Man's grid coordinates
        private int dx, dy;                // movement direction
        private boolean gameOver = false;
        private boolean win = false;

        // Ghosts
        private final List<Ghost> ghosts = new ArrayList<>();

        // Timer for game updates
        private final Timer timer;

        // Random number generator
        private final Random rand = new Random();

        public GamePanel(int numBlocks) {
            this.NUM_BLOCKS = numBlocks;
            this.SCREEN_SIZE = NUM_BLOCKS * BLOCK_SIZE;

            setPreferredSize(new Dimension(SCREEN_SIZE, SCREEN_SIZE));
            setBackground(Color.black);
            setFocusable(true);
            addKeyListener(this);

            initMaze();
            initCharacters();

            // Game loop timer (updates every 150ms)
            timer = new Timer(150, this);
            timer.start();
        }

        // Generate a maze with solid borders and random interior cells.
        private void initMaze() {
            maze = new int[NUM_BLOCKS][NUM_BLOCKS];
            for (int i = 0; i < NUM_BLOCKS; i++) {
                for (int j = 0; j < NUM_BLOCKS; j++) {
                    if (i == 0 || i == NUM_BLOCKS - 1 || j == 0 || j == NUM_BLOCKS - 1) {
                        maze[i][j] = 1;  // border wall
                    } else {
                        // Probability for an interior cell to be a wall
                        double WALL_PROBABILITY = 0.25;
                        maze[i][j] = (rand.nextDouble() < WALL_PROBABILITY) ? 1 : 2;
                    }
                }
            }
        }

        // Initialize characters, then fill in any unreachable blocks as walls.
        private void initCharacters() {
            // Place Pac-Man at the center.
            pacmanX = NUM_BLOCKS / 2;
            pacmanY = NUM_BLOCKS / 2;
            maze[pacmanX][pacmanY] = 0;

            // Compute reachable cells from Pac-Man's starting position.
            List<int[]> reachable = getReachableCells(pacmanX, pacmanY);

            // For any cell that is not reachable, convert it to a wall.
            boolean[][] visited = new boolean[NUM_BLOCKS][NUM_BLOCKS];
            for (int[] cell : reachable) {
                visited[cell[0]][cell[1]] = true;
            }
            for (int i = 0; i < NUM_BLOCKS; i++) {
                for (int j = 0; j < NUM_BLOCKS; j++) {
                    if (!visited[i][j]) {
                        maze[i][j] = 1;  // fill unreachable cells with wall
                    }
                }
            }

            // Choose ghost spawn positions from reachable cells (excluding Pac-Man's cell).
            List<int[]> potentialGhostSpawns = new ArrayList<>();
            for (int[] cell : reachable) {
                if (!(cell[0] == pacmanX && cell[1] == pacmanY)) {
                    potentialGhostSpawns.add(cell);
                }
            }
            ghosts.clear();
            if (potentialGhostSpawns.size() >= 2) {
                int index1 = rand.nextInt(potentialGhostSpawns.size());
                int[] pos1 = potentialGhostSpawns.get(index1);
                potentialGhostSpawns.remove(index1);
                int index2 = rand.nextInt(potentialGhostSpawns.size());
                int[] pos2 = potentialGhostSpawns.get(index2);
                ghosts.add(new Ghost(pos1[0], pos1[1], Color.red));
                ghosts.add(new Ghost(pos2[0], pos2[1], Color.pink));
                maze[pos1[0]][pos1[1]] = 0;
                maze[pos2[0]][pos2[1]] = 0;
            } else {
                // Fallback positions if not enough reachable cells.
                ghosts.add(new Ghost(1, 1, Color.red));
                ghosts.add(new Ghost(NUM_BLOCKS - 2, NUM_BLOCKS - 2, Color.pink));
                maze[1][1] = 0;
                maze[NUM_BLOCKS - 2][NUM_BLOCKS - 2] = 0;
            }
        }

        // BFS to get a list of reachable cells (each cell represented as int[]{x, y})
        private List<int[]> getReachableCells(int startX, int startY) {
            List<int[]> reachable = new ArrayList<>();
            boolean[][] visited = new boolean[NUM_BLOCKS][NUM_BLOCKS];
            Queue<int[]> queue = new LinkedList<>();
            queue.add(new int[]{startX, startY});
            visited[startX][startY] = true;
            reachable.add(new int[]{startX, startY});
            int[] dxArr = {-1, 1, 0, 0};
            int[] dyArr = {0, 0, -1, 1};
            while (!queue.isEmpty()) {
                int[] cell = queue.poll();
                int cx = cell[0], cy = cell[1];
                for (int i = 0; i < 4; i++) {
                    int nx = cx + dxArr[i];
                    int ny = cy + dyArr[i];
                    if (nx >= 0 && nx < NUM_BLOCKS && ny >= 0 && ny < NUM_BLOCKS &&
                            !visited[nx][ny] && maze[nx][ny] != 1) {
                        visited[nx][ny] = true;
                        int[] newCell = {nx, ny};
                        reachable.add(newCell);
                        queue.add(newCell);
                    }
                }
            }
            return reachable;
        }

        // Main game loop: update positions, check collisions, and check win condition.
        @Override
        public void actionPerformed(ActionEvent e) {
            if (!gameOver && !win) {
                movePacman();
                for (Ghost ghost : ghosts) {
                    ghost.move();
                    if (ghost.x == pacmanX && ghost.y == pacmanY) {
                        gameOver = true;
                        timer.stop();
                    }
                }
                // Check if any pellet remains.
                boolean pelletExists = false;
                for (int i = 0; i < NUM_BLOCKS; i++) {
                    for (int j = 0; j < NUM_BLOCKS; j++) {
                        if (maze[i][j] == 2) {
                            pelletExists = true;
                            break;
                        }
                    }
                    if (pelletExists) break;
                }
                if (!pelletExists) {
                    win = true;
                    timer.stop();
                    int choice = JOptionPane.showOptionDialog(
                            this,
                            "You Win!\nChoose an option:",
                            "Victory!",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.INFORMATION_MESSAGE,
                            null,
                            new String[]{"Exit", "New Level"},
                            "New Level"
                    );
                    if (choice == 0) {
                        System.exit(0);
                    } else {
                        // Restart by disposing and creating a new frame.
                        PacmanGame.this.dispose();
                        new PacmanGame();
                    }
                }
            }
            repaint();
        }

        // Move Pac-Man if the next cell is not a wall.
        private void movePacman() {
            int newX = pacmanX + dx;
            int newY = pacmanY + dy;
            if (newX >= 0 && newX < NUM_BLOCKS && newY >= 0 && newY < NUM_BLOCKS) {
                if (maze[newX][newY] != 1) {
                    pacmanX = newX;
                    pacmanY = newY;
                    if (maze[pacmanX][pacmanY] == 2) {
                        maze[pacmanX][pacmanY] = 0;
                    }
                }
            }
        }

        // Rendering: draw the maze, Pac-Man, ghosts, and game-over message.
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            for (int i = 0; i < NUM_BLOCKS; i++) {
                for (int j = 0; j < NUM_BLOCKS; j++) {
                    int cell = maze[i][j];
                    if (cell == 1) {
                        g.setColor(Color.blue);
                        g.fillRect(i * BLOCK_SIZE, j * BLOCK_SIZE, BLOCK_SIZE, BLOCK_SIZE);
                    } else if (cell == 2) {
                        g.setColor(Color.white);
                        int pelletSize = BLOCK_SIZE / 3;
                        g.fillOval(i * BLOCK_SIZE + (BLOCK_SIZE - pelletSize) / 2,
                                j * BLOCK_SIZE + (BLOCK_SIZE - pelletSize) / 2,
                                pelletSize, pelletSize);
                    }
                }
            }
            g.setColor(Color.yellow);
            g.fillOval(pacmanX * BLOCK_SIZE, pacmanY * BLOCK_SIZE, BLOCK_SIZE, BLOCK_SIZE);
            for (Ghost ghost : ghosts) {
                g.setColor(ghost.color);
                g.fillOval(ghost.x * BLOCK_SIZE, ghost.y * BLOCK_SIZE, BLOCK_SIZE, BLOCK_SIZE);
            }
            if (gameOver) {
                g.setColor(Color.white);
                g.setFont(new Font("Arial", Font.BOLD, 36));
                String msg = "GAME OVER";
                int msgWidth = g.getFontMetrics().stringWidth(msg);
                g.drawString(msg, (SCREEN_SIZE - msgWidth) / 2, SCREEN_SIZE / 2);
            }
        }

        // Key listener methods.
        @Override
        public void keyPressed(KeyEvent e) {
            int key = e.getKeyCode();
            if (!gameOver && !win) {
                dx = 0;
                dy = 0;
                if (key == KeyEvent.VK_LEFT)  dx = -1;
                if (key == KeyEvent.VK_RIGHT) dx =  1;
                if (key == KeyEvent.VK_UP)    dy = -1;
                if (key == KeyEvent.VK_DOWN)  dy =  1;
            }
        }
        @Override public void keyReleased(KeyEvent e) { }
        @Override public void keyTyped(KeyEvent e) { }

        // ----------------------------------------------------
        // Inner class representing a ghost with random movement.
        // ----------------------------------------------------
        private class Ghost {
            int x, y;
            int dx, dy;
            Color color;

            Ghost(int x, int y, Color color) {
                this.x = x;
                this.y = y;
                this.color = color;
                setRandomDirection();
            }

            private void setRandomDirection() {
                int r = rand.nextInt(4);
                switch (r) {
                    case 0 -> {
                        dx = 1;
                        dy = 0;
                    }
                    case 1 -> {
                        dx = -1;
                        dy = 0;
                    }
                    case 2 -> {
                        dx = 0;
                        dy = 1;
                    }
                    case 3 -> {
                        dx = 0;
                        dy = -1;
                    }
                }
            }

            void move() {
                int newX = x + dx;
                int newY = y + dy;
                if (newX < 0 || newX >= NUM_BLOCKS || newY < 0 || newY >= NUM_BLOCKS ||
                        maze[newX][newY] == 1) {
                    setRandomDirection();
                } else {
                    x = newX;
                    y = newY;
                }
            }
        }
    }
}
