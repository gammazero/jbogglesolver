/**
* Find words in 4x4 boggle grids and display results interactively.
*
* This script uses the bogglesolver module to generate solutions to the boggle
* grids entered by a user.  The bogglesolver's internal dictionary is created
* once when the object is initialized.  It is then reused for subsequent
* solution searches.
*
* The user is prompted to input a string of x*y characters, representing the
* letters in a X by Y Boggle grid.  Use the letter 'q' to represent "qu".
*
* For example: "qadfetriihkriflv" represents the 4x4 grid:
* +---+---+---+---+
* | Qu| A | D | F |
* +---+---+---+---+
* | E | T | R | I |
* +---+---+---+---+
* | I | H | K | R |
* +---+---+---+---+
* | I | F | L | V |
* +---+---+---+---+
*
* This grid has 62 unique solutions using the default dictionary.
*
* Display help to see usage infomation: java Boggle
*
* AUTHOR:
* Andrew Gillis - 23 Dec 2009, 08 Feb 2011
*
*/

import java.io.*;
import java.util.*;

class StringLengthComparator implements Comparator<String> {

    private final boolean longest;

    public StringLengthComparator(boolean longest) {
        this.longest = longest;
    }

    public int compare(String o1, String o2) {
        if (o1.length() < o2.length()) {
            if (longest)
                return 1;
            return -1;
        }
        if (o1.length() > o2.length()) {
            if (longest)
                return -1;
            return 1;
        }
        return 0;
    }
}

public class Boggle {

    private static final int ALPHA=0;
    private static final int LONGEST=1;
    private static final int SHORTEST=2;
    private static final String DEFAULT_WORDS="boggle_dict.txt.gz";

    public static void main(String[] argv) {
        int xlen = 4;
        int ylen = 4;
        int sortType = ALPHA;
        int quietLevel = 0;
        boolean benchmark = false;

        String usageMsg = "usage: java Boggle [option].. [-x width] "+
            "[-y height] [words_file]";

        boolean helpOpt = false;
        boolean preCalcAdj = false;
        String errMsg = null;
        String wordsFile = null;
        String infile = null;

        for (int i=0, argc = argv.length; argc > 0;) {
            String arg;
            arg = argv[i++];
            argc--;
            if (arg.startsWith("-")) {
                if (arg.equals("-h") || arg.equals("--help")) {
                    helpOpt = true;
                } else if (arg.equals("-l") || arg.equals("--longest")) {
                    sortType = LONGEST;
                } else if (arg.equals("-s") || arg.equals("--shortest")) {
                    sortType = SHORTEST;
                } else if (arg.equals("-x")) {
                    if (0 == argc) {
                        errMsg = "Missing X length (height) of board";
                        break;
                    }
                    arg = argv[i++];
                    xlen = Integer.valueOf(arg);
                    argc--;
                } else if (arg.equals("-y")) {
                    if (0 == argc) {
                        errMsg = "Missing Y length (height) of board";
                        break;
                    }
                    arg = argv[i++];
                    ylen = Integer.valueOf(arg);
                    argc--;
                } else if (arg.equals("-f")) {
                    if (0 == argc) {
                        errMsg = "Missing input file name.";
                        break;
                    }
                    infile = argv[i++];
                    argc--;
                } else if (arg.equals("-p")) {
                    preCalcAdj = true;
                } else if (arg.equals("-qq")) {
                    quietLevel = 2;
                } else if (arg.equals("-q")) {
                    quietLevel = 1;
                } else if (arg.equals("-b")) {
                    benchmark = true;
                } else {
                    errMsg = "Unknown option: "+arg;
                    break;
                }
            } else {
                wordsFile = arg;
            }
        }

        if (errMsg != null) {
            System.out.println(errMsg);
            System.out.println(usageMsg);
            System.out.println("Try 'java Boggle -h' for more information.");
            return;
        }

        if (helpOpt) {
            System.out.println(usageMsg);
            System.out.println(
                "-b      : run benchmark test\n"+
                "-f file : file to read characters of board from\n"+
                "-h      : print this help message and exit (also --help)\n"+
                "-l      : sort words longest-first\n"+
                "-p      : pre-calculate adjacency matrix\n"+
                "-q      : do not display grid\n"+
                "-qq     : do not display grid or solutions\n"+
                "-s      : sort words shortest-first\n"+
                "-x len  : Width (X-length) of board.\n"+
                "-y len  : Height (Y-length) of board.\n"+
                "\nDefault values:\n"+
                "If -l or -s not specified, then words are sorted "+
                "alphabetically.\n"+
                "If -x is not specified, then x-length is set to 4.\n"+
                "If -y is not specified, then y-length is set to 4.\n"+
                "If no words file is given, then use "+DEFAULT_WORDS);
            return;
        }

        runBoard(wordsFile, xlen, ylen, sortType, infile, quietLevel,
                 benchmark, preCalcAdj);
        return;
    }

    private static void runBoard(String wordsFile, int xlen, int ylen,
                                 int sortType, String inFile, int quietLevel,
                                 boolean benchmark, boolean preCalcAdj) {
        if (wordsFile == null) {
            wordsFile = DEFAULT_WORDS;
        }

        BoggleSolver solver = new BoggleSolver(xlen, ylen, wordsFile,
                                               preCalcAdj);
        int boardSize = solver.boardSize();
        if (-1 == boardSize) {
            return;
        }

        while(true) {
            String grid;

            if (benchmark) {
                StringBuilder sbgrid = new StringBuilder(boardSize);
                int c = 0;
                while (sbgrid.length() < boardSize) {
                    if (c == 26) {
                        c = 0;
                    }
                    sbgrid.append((char)('a' + c));
                    ++c;
                }
                grid = sbgrid.toString();
            } else if (null == inFile) {
                grid = readGridFromUser(boardSize);
            } else {
                grid = readGridFromFile(boardSize, inFile);
            }

            if (grid == null || grid.length() == 0) {
                break;
            }

            long start = System.nanoTime();
            Set<String> wordSet = solver.solve(grid);
            long elapsed = System.nanoTime() - start;

            // If invalid grid, then ask for input again.
            if (wordSet == null) {
                continue;
            }

            String[] words = wordSet.toArray(new String[]{});
            double msec = (double) elapsed / (double) 1000000;
            System.out.format("\nFound %d solutions for %dx%d grid "+
                              "in %.2f msec:", words.length, xlen, ylen, msec);

            if (quietLevel < 2) {
                if (quietLevel < 1) {
                    solver.showGrid(grid);
                }
                showWords(words, sortType);
            }

            if (benchmark) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    continue;
                }
            }
        }
    }

    private static String readGridFromUser(int boardSize) {
        System.out.format("\nEnter %d letters from boggle grid: ", boardSize);

        BufferedReader stdin = new BufferedReader(
            new InputStreamReader(System.in));

        StringBuilder chars = new StringBuilder(boardSize);
        String line;
        while (chars.length() < boardSize) {
            try {
                line = stdin.readLine();
                if (0 == line.length()) {
                    return null;
                }
                chars.append(line);
            } catch (java.io.IOException ioe) {
                System.err.println("I/O Error " + ioe);
                return null;
            }
            if (chars.length() < boardSize) {
                System.out.format("\n%d more letters needed: ",
                                  boardSize - chars.length());
            }
        }
        chars.setLength(boardSize);
        return chars.toString();
    }

    private static String readGridFromFile(int boardSize, String fileName) {
        System.out.format("\nReading %d letters from boggle grid file: %s",
                          boardSize, fileName);
        return null;
    }

    private static void showWords(String[] words, int sortType) {
        // Sort words alphabetically
        Arrays.sort(words);

        if (sortType == LONGEST) {
            // Sort words by length, longest to shortest.
            Arrays.sort(words, new StringLengthComparator(true));
        }
        else if (sortType == SHORTEST) {
            // Sort words by length, shortest to longest.
            Arrays.sort(words, new StringLengthComparator(false));
        }

        List<String> sortedWords = new LinkedList<String>(
            Arrays.asList(words));

        // Display words in 4 columns (assumes 80-char wide display).
        String w1, w2, w3, w4;
        while(!sortedWords.isEmpty()) {
            w1 = sortedWords.remove(0);
            w2 = w3 = w4 = "";
            if (!sortedWords.isEmpty()) {
                w2 = sortedWords.remove(0);
            }
            if (!sortedWords.isEmpty()) {
                w3 = sortedWords.remove(0);
            }
            if (!sortedWords.isEmpty()) {
                w4 = sortedWords.remove(0);
            }
            System.out.format("%-18s %-18s %-18s %-18s\n", w1, w2, w3, w4);
        }
    }

}
