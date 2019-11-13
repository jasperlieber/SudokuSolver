package com.vj.sudoku.v1;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;


public class Game extends Activity implements Runnable {

	private PuzzleView puzzleView;
	private int puzzle[] = new int[9*9];

	// sudoku predefinito ** default sudoku
	private final String puzz = "000000000000000000000000000000000000000000000000000000000000000000000000000000000";
	private final String puzz_vuoto = "000000000000000000000000000000000000000000000000000000000000000000000000000000000";
	// caching del sudoku riempito dall'utente
	@SuppressWarnings("unused")
	private String puzz_iniziale = "000000000000000000000000000000000000000000000000000000000000000000000000000000000";
	// boolean sulla consistenza
	// array per il caching delle celle usate *** used cells
	private final int used[][][] = new int[9][9][];
    // contatore di backtrack per la soluzione
	private int backtrack;
    private boolean first;
    private ProgressDialog pd;
    private JasperSolver _js = null;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		puzzle = getPuzzle();
		calculateUsedTiles();

		puzzleView = new PuzzleView(this);
		setContentView(puzzleView);
		puzzleView.requestFocus();

		first=true;

	}

	// menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

    	super.onCreateOptionsMenu(menu);
    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.menu, menu);

    	return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

    	switch(item.getItemId()) {

    	case R.id.solve:

    		pd = ProgressDialog.show(this, "Sudoku Solver", "Solving...", true, false);

    		Thread thread = new Thread(this);
    		thread.start();

    		return true;


    	case R.id.restart:
    		// setta la tavola a zero
    		puzzle = getEmptyPuzzle();
    		// ridisegna lo schermo
    		puzzleView.invalidate();
    		// pulisci l'array delle celle usate
    		calculateUsedTiles();
    		return true;


    	case R.id.exit:
    		// chiudi l'activity
    		this.setResult(RESULT_CANCELED);
    		finish();
    		return true;


    	}

    	return false;
    }



    private boolean solveSudoku() {

//        debugging test run with following puzzle
//        puzzle = fromPuzzleString( "600800005059600700000050900000007040760000051080400000007010000002004190400002003" );

        _js = new JasperSolver(puzzle);

        try {
            puzzle = _js.solveAndReturnPuzzle();
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("JasperSudoku", e.getMessage());
        }


        return is_Solved();
    }

//        showToastShort("Jasper processing done");
//
//        // ** IF YOUR METHOD FAILS TO SOLVE THE SUDOKU, USE MY BACKTRACK
//        // ALGORITHM ***
//        if (!is_Solved()) {
//		   // if sudoku wasn't solved, use backtrack algorithm
//		   return backtrack(0, 0);
//	   }
//
//       return false;
//   }


   private boolean backtrack(int i, int j) {

	   // limiti *** limits
	   if (i == 9) {
            i = 0;
            if (++j == 9)
                return true;

        }

	   // se la cella è riempita, saltala *** if cell is not empty, ignore it
        if (getTile(i, j) != 0) {
        	return backtrack(i+1, j);
        } else {
        	// se è la prima casella da riempire, salva le coordinate *** if this is the first cell to fill, save the coords
        	if(first) {
        		calculateUsedTiles();
        		first=false;

        	}

        }

        // riempimento celle bruteforce+backtracking
        for(int x=1; x <= 9; x++) {

        	calculateUsedTiles(i, j);
        	if(setTileIfValid(i, j, x)) {

 					if (backtrack(i + 1, j)) {
						return true;
				}
        	}
        }

        // backtrack
        backtrack++;
        setTile(i, j, 0);
        // visualizza numero backtrack sulla progress bar
        handler.sendEmptyMessage(backtrack);

        return false;

   }

	private boolean is_Solved() {
		// controlla se è tutto riempito
		// check if it has been solved
		int g[] = getActualPuzzle();
		int nused = 0;
		for(int t : g) {
			if(t==0) nused++;
		}

		//puzzleView.invalidate();

		if(nused==0) return true;
		else return false;

	}

	private void saveSudoku() {
		puzz_iniziale = toPuzzleString(puzzle);
	}

	// keypad
	protected void showKeypadOrError(int x, int y) {
		int tiles[] = getUsedTiles(x, y);
		if(tiles.length == 9) {
			Toast toast = Toast.makeText(this, R.string.no_moves_label, Toast.LENGTH_SHORT);
			toast.setGravity(Gravity.CENTER, 0, 0);
			toast.show();
		} else {
			Dialog v = new Keypad(this, tiles, puzzleView);
			v.setTitle(R.string.keypad_title);
			v.show();
		}
	}

	// cambia il valore della cella solo se è valido
	// sets the value of the cell only it is valid
	protected boolean setTileIfValid(int x, int y, int value) {

		int tiles[] = getUsedTiles(x, y);
		if(value != 0) {
			for(int tile : tiles) {
				if(tile == value) return false;
			}
		}
		setTile(x, y, value);
		calculateUsedTiles();
		return true;
	}

	protected int[] getUsedTiles(int x, int y) {
		return used[x][y];
	}

	// lancia il calcolo delle celle usate per ognuna

	private void calculateUsedTiles() {
		for(int x=0; x<9; x++) {
			for(int y=0; y<9; y++) {
				used[x][y] = calculateUsedTiles(x, y);

			}
		}
	}

	// controllo celle usate
	private int[] calculateUsedTiles(int x, int y) {
		int c[] = new int[9];

		// orizzonali
		for(int i=0; i<9; i++) {
			if(i == y) continue;
			int t = getTile(x, i);
			if(t!=0) c[t-1] = t;
		}

		// verticali
		for(int i=0; i<9; i++) {
			if(i==x) continue;
			int t = getTile(i, y);
			if(t!=0) c[t-1] = t;

		}

		// stesso blocco 3x3
		int startx = (x/3) *3;
		int starty = (y/3) *3;

		for(int i = startx; i<startx+3; i++) {
			for(int j = starty; j< starty+3; j++) {
				if(i==x && j==y) continue;
				int t = getTile(i, j);
				if(t!=0) c[t-1]= t;
			}
		}

		// elimino gli zeri in modo da poter usare il lenght() sull'array
		int nused = 0;
		for(int t : c) {
			if(t!=0) nused++;
		}

		int c1[] = new int[nused];
		nused = 0;
		for (int t : c) {
			if(t!=0) c1[nused++] = t;
		}

		return c1;
	}

	private int getTile(int x, int y) {
		return puzzle[y*9 + x];
	}

	private void setTile( int x, int y, int value) {
		puzzle[y*9 + x] = value;
		calculateUsedTiles();
	}

	private int[] getPuzzle() {
		return fromPuzzleString(puzz);
	}

	private int[] getActualPuzzle() {
		return puzzle;
	}

	private int[] getEmptyPuzzle() {
		return fromPuzzleString(puzz_vuoto);
	}

	private int[] fromPuzzleString(String string) {
		int[] puz = new int[string.length()];
		for(int i=0; i< puz.length; i++) {
			puz[i] = string.charAt(i) - '0';
		}
		return puz;
	}

	static private String toPuzzleString(int[] puz) {
		StringBuilder buf = new StringBuilder();
		for(int element : puz) {
			buf.append(element);
		}
		return buf.toString();
	}

	protected String getTileString( int x, int y) {
		int v = getTile(x, y);
		if( v==0 ) return "";
		else return String.valueOf(v);
	}

	private void showToastShort(String p) {
		Toast toast = Toast.makeText(this, p, Toast.LENGTH_SHORT);
		toast.setGravity(Gravity.CENTER, 0, 0);
		toast.show();
	}

	private final Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {

			if(msg.what==0) {
				pd.dismiss();
				puzzleView.invalidate();
				if( is_Solved() ) showToastShort( getString(R.string.result) +" "+backtrack);
				else showToastShort( getString(R.string.noresult) +" "+backtrack);
			} else
				pd.setMessage("backtracking: "+backtrack);
		}
	};

	@Override
	public void run() {

		Looper.prepare();
		// salva il sudoku inserito dall'utente
		saveSudoku();
		// risolvi
		backtrack=0;
		solveSudoku();

		handler.sendEmptyMessage(0);

	}

    public boolean tileCouldBe(int i, int j, int k) {
        if ( _js == null ) return true;
        return _js.tileCouldBe(i,j,k);
    }



}


