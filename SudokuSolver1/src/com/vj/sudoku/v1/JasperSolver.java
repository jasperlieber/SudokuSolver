package com.vj.sudoku.v1;

import java.util.TreeSet;

import android.util.Log;

/**
 * Implements a way to hold Sudoku puzzles, and solve them, using only
 * a short list of rules (as opposed to a brute force search for solutions).
 * The solution algorithm is to iteratively pass through each cell of a
 * puzzle, applying the rules to see if possibilities can be eliminated, doing so
 * until the puzzle is no longer changing (or is discovered to be invalid).
 * <br><br>
 * Each cell has a "remains" set, contains the possibilities for that cell.
 * There are some basic rules used to whittle the remains set down, as follows:
 * <ul>
 * <li>If a cell contains a single value, remove that value from the remains
 * for other cells in its row, column, and subsquare.
 * <li>Remove all the remains of other cells in this row, column, and subsquare.
 * <li>If there are only 3 possibilities for a 3-cell row or column
 * of an 3x3 square, those three values must go in that row or column,
 * and we can eliminate those possibilities for all cells in other rows
 * of this square and of all cells in this same row in other squares.
 * <li>Given the corners of a 3x3 square, run some inter-square logic:
 * <br>Check each [1 x 3] row for values that are only in that row
 * and remove them from other square's row.
 * Do same for columns.
 * <br>
 * If rowRemains or colRemains is just 3 numbers, then the row or col must
 * contain just those three numbers, and the other rows or columns can
 * have those values subtracted from their remains.
 * </ul>
 * Whenever any of the rules reduces a cell to just a single number, that value
 * can be eliminated from the remains for other row, columns and subsquares.
 * <br><br>
 * The algorithm stops iterating when no further changes are detected, and
 * the puzzle is either solved or not solvable with these rules (or was
 * determined to be invalid and an exception thrown).
 * <br><br>
 * @author Jasper Lieber
 *
 */
public class JasperSolver {

    private static final boolean _debugEnabled = false;

    /**
     * SSet implements set operations on the integers 1-9.
     * Note that this implementation is quite heavy but is quick & easy.
     *
     */
    private class SSet extends TreeSet<Integer> {
        private static final long serialVersionUID = 1L;

        /**
         * Construct a SSet, either empty or full of 1-9.
         * @param fill if true, populate the set with 1-9
         */
        public SSet(boolean fill) {
            super();
            if (fill)
                for (int val=1; val < 10; val++)
                    this.add(val);
        }

        /**
         * Construct a SSet with the one value in it
         * @param value the one value to place in the SSet
         */
        public SSet(Integer value) {
            super();
            this.add(value);
        }

        /**
         * Construct a SSet, and add all the values of the passed set.
         * @param sset a set of values to add to the new set
         */
        public SSet(SSet sset) {
            super();
            this.addAll(sset);
        }

        /**
         * Return the size of the SSet.  I had to override this method because
         * super.size() is returning wrong values when running the
         * Android emulator.  (This did not occur with my original
         * Java app version.)
         */
        @Override
        public int size() {

            int cnt = 0;
            for (int val=1; val < 10; val++)
                if ( this.contains( val )) cnt++;

            // TODO - the following illustrates strangeness on my system
            if (cnt != super.size())
            {
                println("craziness!  android java bug?  this = " + this.toString() +
                        ", and cnt = " + cnt +
                        ", and super.size() = " + super.size() +
                        ".   why different?");
            }
            return cnt;

        }

        /**
         * Return a pretty printed String of values in the SSet
         */
        @Override
        public String toString() {
            StringBuffer str = new StringBuffer(9);
            for (int val=1; val < 10; val++)
                str.append(this.contains(val) ? val : "-");
            return str.toString();
        }

    }

    public class UnsolvableException extends Exception
    {
        private static final long serialVersionUID = 1L;
        public UnsolvableException(String str)
        {
            super(str);
        }
    }

    private static final boolean FILLED = true;
    private static final boolean EMPTY = false;


    private int m_steps = 0;

    private final int[][]   _puzzle = new int[9][9];

    private final SSet[]    _rowSets = new SSet[9];
    private final SSet[]    _colSets = new SSet[9];
    private final SSet[][]  _sqrSets = new SSet[3][3];
    private final SSet[]    _solos = new SSet[10];
    private final SSet[][]  _remains = new SSet[9][9];


    private JasperSolver() {
        for (int row = 0; row < 9; row++)
            for (int col = 0; col < 9; col++) {
                _puzzle[row][col] = 0;
                _remains[row][col] = new SSet(FILLED);
            }


        for (int row = 0; row < 9; row++) {
            _rowSets[row] = new SSet(FILLED);
            _colSets[row] = new SSet(FILLED);
            _solos[row+1] = new SSet(row+1);
        }

        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 3; col++)
                _sqrSets[row][col] = new SSet(FILLED);

        m_steps = 0;
    }

    public JasperSolver( int[] puzzle ) {
        this();
        for (int row = 0; row < 9; row++)
            for (int col = 0; col < 9; col++) {
                int value = puzzle[ row*9 + col ];
                if ( value != 0 ) {
                    _puzzle[row][col] = value;
                    _remains[row][col] = _solos[value];
                    _rowSets[row].remove(value);
                    _colSets[col].remove(value);
                    _sqrSets[row/3][col/3].remove(value);
                }
            }
    }


    private int[] ssToIntArray() {
        int puzzle[] = new int[9*9];
        for (int row = 0; row < 9; row++)
            for (int col = 0; col < 9; col++)
                puzzle[ row*9 + col ] = _puzzle[row][col];
        return puzzle;
    }

    private void verify(){
		SSet sset = null;

		try {
			for (int row = 0; row < 9; row++) {
				sset = new SSet(EMPTY);
				for (int col = 0; col < 9; col++) {
					if (_puzzle[row][col] != 0) {
						if (sset.contains(_puzzle[row][col]))
								throw new Exception("verify failed - step #" + m_steps
										+ " [row col] = " + (row+1) + " " + (col+1));
						sset.add(_puzzle[row][col]);
					}
				}
			}

			for (int col = 0; col < 9; col++) {
				sset = new SSet(EMPTY);
				for (int row = 0; row < 9; row++) {
					if (_puzzle[row][col] != 0) {
						if (sset.contains(_puzzle[row][col]))
							throw new Exception("verify failed - step #" + m_steps
									+ " [col row] = " + col + " " + row);
						sset.add(_puzzle[row][col]);
					}
				}
			}
		} catch (Exception e) {
			println("Validate failed - " + e.getMessage());
			printPuzzle();
			printRemains();
			System.exit(-1);
		}
	}



    public void printPuzzle() {
        String out = "";
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++)
                out += ((_puzzle[row][col]==0 ? "-" : _puzzle[row][col]) + " ");
            println(out);
            out = "";
        }
        println("");
    }

    public void printRemains() {
        println("\nremains:\n");
        String out = "";
        for (int row = 0; row < 9; row++) {
            if (row == 3 || row == 6) {
                out += ("          ");
                for (int col = 0; col < 8; col++) {
                    for (int nn = 0; nn < 9; nn++)
                        out += ("=");
                    out += ((col == 2 || col == 5) ? "++" : "==");
                }
                out += ("=========");
                println(out);
                out = "";
            }

            out += ("          ");
            for (int col = 0; col < 9; col++)
                out += (_remains[row][col] + ((col == 2 || col == 5) ? "||" : "  "));
            println(out);
            out = "";
        }
        println("");

//      print("\nsets:\n          ");
//      for (int col = 0; col < 9; col++)
//          print(colSets[col] + "  ");
//      println();
//      for (int row = 0; row < 9; row++) {
//          print(rowSets[row]);
//          if ((row - 1) % 3 == 0) {
//              for (int col = 0; col < 3; col++)
//                  print("           " + sqrSets[row/3][col] + "          ");
//          }
//          println();
//      }
//      println();
    }


    /**
     * The correct value for a cell has been found.  Remove that value from
     * the possibilities for the row, the column, and the 3x3 square.  Also,
     * set the value in SS, and in remains.
     *
     * @param row
     * @param col
     * @param val - the value for the cell at row & col
     */
    private void processDiscovery(int row, int col, int val) {
//		if (verbose) {
//			printPuzzle();
//			printRemains();
//		}
        _rowSets[row].remove(val);
        _colSets[col].remove(val);
        _sqrSets[row/3][col/3].remove(val);
        _puzzle[row][col] = val;
        _remains[row][col] = new SSet(val);
    }

    /**
     * If there are only 3 possibilities for a 3-cell row or column
     * of an 3x3 square, those three values must go in that row or column,
     * and we can eliminate those possibilities for all cells in other rows
     * of this square and of all cells in this same row in other squares.
     *
     * @param row
     * @param col
     * @return
     * @throws Exception - the puzzle turns out to be unsolvable.
     */
    private boolean checkThrees(int row, int col)
        throws UnsolvableException
    {
        int rowStart = row/3;
        int colStart = col/3;

        boolean changing = false;

        // see if only 3 left in this row
        SSet only3 = new SSet(EMPTY);
        only3.addAll(_remains[row][colStart * 3]);
        only3.addAll(_remains[row][colStart * 3 + 1]);
        only3.addAll(_remains[row][colStart * 3 + 2]);

        if (only3.size() == 3) {
            // remove the only3 set from the remains
            // of all cells in other rows of this square and
            // of all cells in this same row in other squares
            for (int jj = rowStart * 3; jj < rowStart * 3 + 3; jj++) {
                for (int kk = 0; kk < 9; kk++) {
                    if (((jj == row) && (kk/3 != colStart)) ||
                        ((jj != row) && (kk/3 == colStart))) {
                        m_steps++;
                        boolean changed = _remains[jj][kk].removeAll(only3);
                        if (changed) {
                            changing = true;
                            checkIsNowOne(jj, kk, "Only Three in a Row - ");
                        }
                    }
                }
            }
        }


        // see if only 3 left in this col
        only3 = new SSet(EMPTY);
        only3.addAll(_remains[rowStart * 3][col]);
        only3.addAll(_remains[rowStart * 3 + 1][col]);
        only3.addAll(_remains[rowStart * 3 + 2][col]);

        if (only3.size() == 3) {
            // remove the only3 set from the remains
            // of all cells in other cols of this square and
            // of all cells in this same col in other squares
            for (int jj = 0; jj < 9; jj++) {
                for (int kk = colStart * 3; kk < colStart * 3 + 3; kk++) {
                    if (((jj/3 == rowStart) && (kk != col)) ||
                        ((jj/3 != rowStart) && (kk == col))) {
                        m_steps++;
                        boolean changed = _remains[jj][kk].removeAll(only3);;
                        if (changed) {
                            changing = true;
                            checkIsNowOne(jj, kk, "Only Three in a Col - ");
                        }
                    }
                }
            }
        }

        return changing;
    }


    /**
     * Check the cell has just one value.  Throw exception if it has no
     * possibilities.
     *
     * @param row
     * @param col
     * @param rule
     * @throws UnsolvableException
     */
    private void checkIsNowOne(int row, int col, String rule)
        throws UnsolvableException
    {
        if (_debugEnabled) {
            println("  Step " + m_steps + ":  Rule " + rule +
                    " discovered remains[" + (row+1) +
                    "][" + (col+1) + "] = " + _remains[row][col]);
            printRemains();
        }

        if (_remains[row][col].size() == 0) {
            printPuzzle();
            printRemains();
            throw new UnsolvableException("Step " + m_steps + ":  " +
                    "[row col] = [" + (row+1) + " " + (col+1) + "] " +
                    "- UNSOLVABLE");
        } else if (_remains[row][col].size() == 1) {
            int last = _remains[row][col].first();
            processDiscovery(row, col, last);
        }
    }

    private boolean checkRowsColsSqrs(int row, int col) throws UnsolvableException {

        boolean hit = false;

        for (int val=1; val < 10 ; val++) {

            if (!_remains[row][col].contains(val)) continue;

            m_steps++;

            boolean rowHit = !_rowSets[row].contains(val);
            boolean colHit = !_colSets[col].contains(val);
            boolean sqrHit = !_sqrSets[row/3][col/3].contains(val);

            hit = rowHit || colHit || sqrHit;
            if (hit) {
                _remains[row][col].remove(val);
                checkIsNowOne(row, col, "Basic Elimination");
            }
            if (_puzzle[row][col] != 0) break;
        }
        return hit;
    }

    // remove the remains of all the other cells in this 3x3 sq
    private boolean checkCanOnlyBeOneSqr(int row, int col) throws UnsolvableException {
        m_steps++;
        SSet onlyOne = new SSet(_remains[row][col]);
        int rowStart = row / 3;
        int colStart = col / 3;
        for (int jj = rowStart * 3; jj < rowStart * 3 + 3; jj++) {
            for (int kk = colStart * 3; kk < colStart * 3 + 3; kk++) {
                if ((jj == row) && (kk == col)) continue;
                onlyOne.removeAll(_remains[jj][kk]);
            }
        }

        if (onlyOne.size() == 1) {
            int val = onlyOne.first();
			if (_debugEnabled)
			    println("Sqr-Remains-Rule");
            processDiscovery(row, col, val);

            return true;
        }

        return false;
    }

    /**
     * From the remains for a cell, remove the remains of all the
     * other cells in the column.  If that leaves only one, process that
     * discovery.
     *
     * @param row
     * @param col
     * @return true if yields one result
     */
    private boolean checkCanOnlyBeOneCheckCol(int row, int col) {
        m_steps++;
        SSet onlyOne = new SSet(_remains[row][col]);
        for (int jj = 0; jj < 9; jj++) {
            if (jj == row)
                continue;
            onlyOne.removeAll(_remains[jj][col]);
        }
        if (onlyOne.size() == 1) {
            int val = onlyOne.first();
            if (_debugEnabled)
                println("Col-Remains-Rule");
//			printPuzzle();
//			printRemains();
            processDiscovery(row, col, val);
            return true;
        }
        return false;
    }

    /**
     * From the remains for a cell, remove the remains of all the
     * other cells in the same row.  If that leaves only one, process that
     * discovery.
     *
     * @param row
     * @param col
     * @return true if yields one result
     */
    private boolean checkCanOnlyBeOneCheckRow(int row, int col)
    {
        m_steps++;
        SSet onlyOne = new SSet(_remains[row][col]);
        for (int jj = 0; jj < 9; jj++) {
            if (jj == col)
                continue;
            onlyOne.removeAll(_remains[row][jj]);
        }
        if (onlyOne.size() == 1) {
            int val = onlyOne.first();
			if (_debugEnabled)
				println("Row-Remains-Rule");
//          printPuzzle();
//          printRemains();
            processDiscovery(row, col, val);
            return true;
        }
        return false;
    }


    /**
     * Given the corners of a 3x3 square, run some inter-square logic:
     * check each [1 x 3] row for values that are only in that row
     * and remove them from other square's row.
     * Do same for columns.
     * <br>
     * If rowRemains or colRemains is just 3 numbers, then the row or col must
     * contain just those three numbers, and the other rows or columns can
     * have those values subtracted from their remains.
     *
     * @param row - corner of a square
     * @param col - corner of a square
     * @return true if any changes happened
     * @throws UnsolvableException
     */
    private boolean cleanOtherSquares(int row, int col) throws UnsolvableException {

        // row & col are corners of a square.
        // check each [1 x 3] row for values that are only in that row
        // and remove them from other square's row.
        // do same for columns
        int rowStart = row / 3;
        int row1 = (rowStart + 1) % 3; row1 *= 3;
        int row2 = (rowStart + 2) % 3; row2 *= 3;
        int colStart = col / 3;
        int col1 = (colStart + 1) % 3; col1 *= 3;
        int col2 = (colStart + 2) % 3; col2 *= 3;
        SSet[] rowContains = new SSet[3];
        SSet[] colContains = new SSet[3];
        for (int jj = 0; jj < 3; jj++) {
            rowContains[jj] = new SSet(EMPTY);
            colContains[jj] = new SSet(EMPTY);
        }
        // build up row & column Contains
        for (int jj = 0; jj < 3; jj++) {
            for (int kk = 0; kk < 3; kk++) {
                rowContains[jj].addAll(_remains[row + jj][col + kk]);
                colContains[kk].addAll(_remains[row + jj][col + kk]);
            }
        }

        // subtract other row & column remains
        SSet[] rowRemains = new SSet[3];
        SSet[] colRemains = new SSet[3];
        for (int jj = 0; jj < 3; jj++) {
            rowRemains[jj] = new SSet(rowContains[jj]);
            colRemains[jj] = new SSet(colContains[jj]);
        }
        for (int nn = 0; nn < 3; nn++) {
            rowRemains[nn].removeAll(rowContains[(nn + 1) % 3]);
            rowRemains[nn].removeAll(rowContains[(nn + 2) % 3]);
            colRemains[nn].removeAll(colContains[(nn + 1) % 3]);
            colRemains[nn].removeAll(colContains[(nn + 2) % 3]);
        }

        // remove leftover values from row & col remains from
        // other square's same rows & cols
        boolean changing = false;
        boolean changed;
        for (int jj = 0; jj < 3; jj++) {
            for (int kk = 0; kk < 3; kk++) {
                m_steps++;
                if ( _debugEnabled ) {
                    println("  doing remains[" + (row + jj) + "]["
                            + (col1 + kk) + "].removeAll(rowRemains[" + jj
                            + "])");
                    println("  doing remains[" + (row1 + kk) + "]["
                            + (col + jj) + "].removeAll(colRemains[" + jj
                            + "])");
                }
                changed = _puzzle[row + jj][col1 + kk] == 0
                        &&                    _remains[row + jj][col1 + kk].removeAll(rowRemains[jj]);
                if (changed) {
                    checkIsNowOne(row + jj, col1 + kk, "rowRemains");
                    changing = true;
                }
                changed = _puzzle[row + jj][col2 + kk] == 0 &&
                    _remains[row + jj][col2 + kk].removeAll(rowRemains[jj]);
                if (changed) {
                    checkIsNowOne(row + jj, col2 + kk, "rowRemains");
                    changing = true;
                }
                changed = _puzzle[row1 + kk][col + jj] == 0 &&
                    _remains[row1 + kk][col + jj].removeAll(colRemains[jj]);
                if (changed) {
                    checkIsNowOne(row1 + kk, col + jj, "colRemains");
                    changing = true;
                }
                changed = _puzzle[row2 + kk][col + jj] == 0 && _remains[row2 + kk][col + jj].removeAll(colRemains[jj]);
                if (changed) {
                    checkIsNowOne(row2 + kk, col + jj, "colRemains");
                    changing = true;
                }

            }
        }

        // if rowRemains or colRemains is just 3 numbers, then the row or col must
        // contain just those three numbers, and the other rows or columns can
        // have those values subtracted from their remains.
        SSet subtract;
        for (int jj = 0; jj < 3; jj++) {
            if (rowRemains[jj].size() == 3) {
                for (int kk = 0; kk < 3; kk++) {
                    if (_puzzle[row + jj][col + kk] != 0) continue;
                    subtract = new SSet(_remains[row + jj][col + kk]);
                    subtract.removeAll(rowRemains[jj]);

                    if ( _debugEnabled ) {
                        println("because rowRemains[" + jj + "] = "
                                + rowRemains[jj] + ", about to subtract "
                                + subtract + " from " + "remains[" + (row + jj)
                                + "][" + (col + kk) + "] ( = "
                                + _remains[row + jj][col + kk] + " )");
                    }

                    changed = _remains[row + jj][col + kk].removeAll(subtract);
                    if (changed) {
                        checkIsNowOne(row + jj, col + kk, "rowRemainsThree");
                        changing = true;
                    }
                }
            }

            if (colRemains[jj].size() == 3) {
                for (int kk = 0; kk < 3; kk++) {
                    if (_puzzle[row + jj][col + kk] != 0) continue;
                    subtract = new SSet(_remains[row + kk][col + jj]);
                    subtract.removeAll(colRemains[jj]);

                    if ( _debugEnabled ) {
                        println("because colRemains["+jj+"] = "
                                + colRemains[jj] + ", about to subtract "
                                + subtract + " from "
                                + "remains["+(row + kk)+"]["+(col + jj)+"] ( = "
                                + _remains[row + kk][col + jj] + " )");
                        println("colRemains["+jj+"].cnt() = " + colRemains[jj].size());
                    }

                    changed = _remains[row + kk][col + jj].removeAll(subtract);
                    if (changed) {
                        checkIsNowOne(row + jj, col + kk, "colRemainsThree");
                        changing = true;
                    }
                }
            }
        }


        return changing;
    }


    /**
     * Iteratively pass through the cells of the puzzle, applying the six
     * rules to each cell, until no further changes occur.  Throw an exception
     * if a rules discovers the puzzle is unsolvable.
     *
     * @throws UnsolvableException
     */
    public void solve() throws UnsolvableException {
        boolean m_changing;
        do {
            m_changing = false;
            for (int row = 0; row < 9; row++) {
                for (int col = 0; col < 9; col++) {
                    if (_puzzle[row][col] == 0)
                        m_changing |= checkRowsColsSqrs(row, col);
                    if (_puzzle[row][col] == 0)
                        m_changing |= checkCanOnlyBeOneSqr(row, col);
                    if (_puzzle[row][col] == 0)
                        m_changing |= checkCanOnlyBeOneCheckCol(row, col);
                    if (_puzzle[row][col] == 0)
                        m_changing |= checkCanOnlyBeOneCheckRow(row, col);
                    if (_puzzle[row][col] == 0)
                        m_changing |= checkThrees(row, col);
                    if ((row % 3) == 0 && (col % 3) == 0) {
                        m_changing |= cleanOtherSquares(row, col);
                    }
                    //verify();
                }
            }
        } while (m_changing);

        println("\nFINAL step " + m_steps + ":");
//      printRemains();
        printPuzzle();
    }

    public int[] solveAndReturnPuzzle() throws UnsolvableException  {
        boolean m_changing;
        do {
            m_changing = false;
            for (int row = 0; row < 9; row++) {
                for (int col = 0; col < 9; col++) {
                    if (_puzzle[row][col] == 0)
                        m_changing |= checkRowsColsSqrs(row, col);
                    if (_puzzle[row][col] == 0)
                        m_changing |= checkCanOnlyBeOneSqr(row, col);
                    if (_puzzle[row][col] == 0)
                        m_changing |= checkCanOnlyBeOneCheckCol(row, col);
                    if (_puzzle[row][col] == 0)
                        m_changing |= checkCanOnlyBeOneCheckRow(row, col);
                    if (_puzzle[row][col] == 0)
                        m_changing |= checkThrees(row, col);
                    if ((row % 3) == 0 && (col % 3) == 0) {
                        m_changing |= cleanOtherSquares(row, col);
                    }
                    if ( _debugEnabled ) {
                        printPuzzle();
                        verify();
                    }
                }
            }
        } while (m_changing);

        if ( _debugEnabled ) {
            println("\nFINAL step " + m_steps + ":");
            printRemains();
            printPuzzle();
        }


        return ssToIntArray();
    }

    private void println(String string) {
        Log.d("J-SUD", string);
    }

    public boolean tileCouldBe(int i, int j, int k) {
        return _remains[i][j].contains(k);
    }



}
