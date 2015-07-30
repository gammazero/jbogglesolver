/**
 * Module to generate solutions for Boggle grids.
 *
 *
 * Andrew Gillis - 23 Dec 2009, 08 Feb 2011
 */

import java.io.*;
import java.util.*;

/**
 * Element of queue constructed while searching word paths on board.
 */
class QNode {
    private final int parentSquare;
    private final String prefix;
    private final Trie parentTrie;
    private final int[] seen;

    QNode(int parentSq, String prefix, Trie parentTrie, int[] seen) {
        this.parentSquare = parentSq;
        this.prefix = prefix;
        this.parentTrie = parentTrie;
        this.seen = seen;
    }

    int getParentSquare() {
        return parentSquare;
    }

    String getPrefix() {
        return prefix;
    }

    Trie getParentTrie() {
        return parentTrie;
    }

    int[] getSeen() {
        return seen;
    }
}

/**
 * Class that implements solver algorithm.
 *
 * This class uses an external words file as a dictionary of acceptable boggle
 * words.  When an instance of this class is created, it sets up an internal
 * dictionary to look up valid boggle answers.  The class' solve method can be
 * used repeatedly to generate solutions for different boggle grids.
 */
public class BoggleSolver {

    private final int rows;
    private final int cols;
    private final int boardSize;
    private Trie root;

    private final int[][] adjacency;

    /**
     * Create and initialize BoggleSolver instance.
     *
     * This creates the internal trie for fast word lookup letter-by-letter.
     * Words that begin with capital letters and words that are not within the
     * specified length limits are filtered out.
     *
     * @param xlen X dimension (width) of board.
     * @param ylen Y dimension (height) of board.
     */
    public BoggleSolver(int xlen, int ylen, boolean preCalcAdjacency) {
        assert(xlen > 1);
        assert(ylen > 1);

        this.boardSize = xlen * ylen;
        this.cols = xlen;
        this.rows = ylen;
        this.root = null;
        if (preCalcAdjacency) {
            this.adjacency = calculateAdjacencyMatrix(xlen, ylen);
        } else {
            this.adjacency = null;
        }
    }

    /**
     * Create and initialize BoggleSolver instance with default values.
     */
    public BoggleSolver() {
        this(4, 4, false);
    }

    /**
     * Return size of board (x * y).
     */
    public int boardSize() {
        return boardSize;
    }

    /**
     * Private method to create the trie for finding words.
     * @param wordsFile Path of file containing words for reference.
     *
     * @return root node of populated trie.
    */
    public int loadDictionary(String wordsFile) {
        System.out.println("creating dictionary...");
        BufferedReader in ;
        try {
            if (wordsFile.endsWith(".gz")) {
                FileInputStream istr = new FileInputStream(wordsFile);
                java.util.zip.GZIPInputStream gzis;
                gzis = new java.util.zip.GZIPInputStream(istr);
                in = new BufferedReader(new InputStreamReader(gzis));
            } else {
                in = new BufferedReader(new FileReader(wordsFile));
            }
        } catch(java.io.FileNotFoundException e) {
            System.err.println("ERROR: unable to open dictionary file: " +
                               wordsFile);
            return 0;
        } catch(IOException e) {
            System.err.println("ERROR: unable to open dictionary file: " +
                               wordsFile);
            return 0;
        }
        root = new Trie();
        String word;
        int wordCount = 0;
        try {
            while ((word = in.readLine()) != null) {
                // Skip words that are too long or too short.
                if (word.length() > boardSize || word.length() < 3) {
                    continue;
                }
                // Skip words that start with capital letter.
                if (word.charAt(0) < 'a') {
                    continue;
                }
                if (word.charAt(0) == 'q') {
                    // Skip words starting with q not followed by u.
                    if (word.charAt(1) != 'u') {
                        continue;
                    }
                    // Remove "u" from q-words so that only the q is matched.
                    word = "q" + word.substring(2);
                }

                //System.out.println("adding word: " + word);
                root.insert(word);
                ++wordCount;
            }
            System.out.println("finished creating dictionary");
        } catch(java.io.IOException e) {
            System.err.println("ERROR: cannot read dictionary file: " +
                               wordsFile);
            root = null;
            wordCount = 0;
        }
        return wordCount;
    }

    /**
     * Generate all solutions for the given boggle grid.
     *
     * @param grid A string of X*Y characters representing the letters in a
     * boggle grid, from top left to bottom right.
     *
     * @return A list of words found in the boggle grid.  None if given invalid
     * grid.
     */
    public Set<String> solve(String grid) {
        if (grid.length() != boardSize) {
            System.err.println("ERROR: invalid board");
            return null;
        }

        char[] board = grid.toLowerCase().toCharArray();
        Trie trie = root;
        Set<String> words = new HashSet<String>();
        Queue<QNode> q = new LinkedList<QNode>();
        int[] adj = new int[8];
        int[] sqAdj = adj;
        int adjCount;
        for (int initSq=0; initSq < boardSize; ++initSq) {
            char c = board[initSq];
            int[] seen = {initSq};
            String s = Character.toString(c);
            QNode qn = new QNode(initSq, s, trie.getChild(c), seen);
            q.offer(qn);
            while (!q.isEmpty()) {
                qn = q.poll();
                int parentSq = qn.getParentSquare();
                String prefix = qn.getPrefix();
                Trie parentTrie = qn.getParentTrie();
                seen = qn.getSeen();
                if (null == adjacency) {
                    adjCount = calcAdjacency(cols, rows, parentSq, adj);
                } else {
                    sqAdj = adjacency[parentSq];
                    adjCount = sqAdj.length;
                }
                for (int a = 0; a < adjCount; ++a) {
                    int curSq = sqAdj[a];
                    boolean hasCur = false;
                    for (int x : seen) {
                        if (x == curSq) {
                            hasCur = true;
                            break;
                        }
                    }
                    if (hasCur) {
                        continue;
                    }
                    c = board[curSq];
                    Trie curNode = parentTrie.getChild(c);
                    if (curNode == null) {
                        continue;
                    }
                    s = prefix + c;
                    int[] newSeen = new int[seen.length + 1];
                    for (int x=0; x < seen.length; ++x) {
                        newSeen[x] = seen[x];
                    }
                    newSeen[seen.length] = curSq;
                    QNode newNode = new QNode(curSq, s, curNode, newSeen);
                    q.offer(newNode);
                    if (curNode.ifIsWord()) {
                        if (s.charAt(0) == 'q') {
                            // Rehydrate q-words with 'u'.
                            words.add("qu" + s.substring(1));
                        } else {
                            words.add(s);
                        }
                    }
                }
            }
        }

        return words;
    }

    /**
     * Utility method to print a X by Y boggle grid.
     *
     * @param grid A string of X*Y characters representing the letters in a
     * boggle grid, from top left to bottom right.
     */
    public void showGrid(String grid) {
        char[] gridChars = grid.toUpperCase().toCharArray();
        int lineLen = 4 * cols + 2;
        StringBuilder line = new StringBuilder(lineLen);
        line.append("+");
        for (int i=0; i < cols; ++i) {
            line.append("---+");
        }
        String hline = line.toString();
        line = new StringBuilder(lineLen);
        line.append("|");
        for (int i=0; i < cols; ++i) {
            line.append("   |");
        }
        System.out.println("");
        for (int y=0; y < rows; ++y) {
            System.out.println(hline);
            int yi = y * cols;
            int lineIndex = 0;
            for (int x=0; x < cols; ++x) {
                char cell = gridChars[yi+x];
                if (cell == 'Q') {
                    line.replace(4 * x + 2, 4 * x + 4, "Qu");
                }
                else {
                    line.setCharAt(4 * x + 2, cell);
                    line.setCharAt(4 * x + 3, ' ');
                }
            }
            System.out.println(line.toString());
        }
        System.out.println(hline);
    }

    /**
     * Create the adjacency matrix for any board dimensions.
     *
     * An array of adjacent squares, up to eight, in calculated for each square
     * on the board.
     *
     * @param xlen X dimension (width) of board.
     * @param ylen Y dimension (height) of board.
     *
     * @return Array of adjacency arrays.
     */
    private static int[][] calculateAdjacencyMatrix(int xlim, int ylim) {
        // Initialize array
        int[][] adjList = new int[ylim*xlim][];
        int[] adj = new int[8];

        for (int sq=0; sq < (xlim * ylim); ++sq) {
            int i = calcAdjacency(xlim, ylim, sq, adj);
            int[] adjfinal = new int[i];
            System.arraycopy(adj, 0, adjfinal, 0, i);
            adjList[sq] = adjfinal;
        }
        return adjList;
    }

    private static int calcAdjacency(int xlim, int ylim, int sq, int[] adj) {
        // Current cell index = y * xlim + x
        int y = sq / xlim;
        int x = sq - (y * xlim);
        int above, below;
        int i = 0;

        // Look at row above current cell.
        if (y-1 >= 0) {
            above = sq - xlim;
            // Look to upper left.
            if (x-1 >=0) {
                adj[i++] = above - 1;
            }
            // Look above.
            adj[i++] = above;
            // Look upper right.
            if (x+1 < xlim) {
                adj[i++] = above + 1;
            }
        }
        // Look at same row that current cell is on.
        // Look to left of current cell.
        if (x-1 >=0) {
            adj[i++] = sq - 1;
        }
        // Look to right of current cell.
        if (x+1 < xlim) {
            adj[i++] = sq + 1;
        }
        // Look at row below current cell.
        if (y+1 < ylim) {
            below = sq + xlim;
            // Look to lower left.
            if (x-1 >= 0) {
                adj[i++] = below - 1;
            }
            // Look below.
            adj[i++] = below;
            // Look to lower rigth.
            if (x+1 < xlim) {
                adj[i++] = below + 1;
            }
        }
        return i;
    }

}
