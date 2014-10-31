package pedCA.environment.grid;

import java.io.IOException;
import java.util.ArrayList;

public abstract class Grid <T>{
	protected ArrayList<ArrayList<GridCell<T>>> cells;
	
	public Grid (int rows,int cols){
		initGrid(rows, cols);
	}
	
	public Grid(String fileName){
		initEmptyGrid();
		try {
			loadFromCSV(fileName);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	protected void initEmptyGrid(){
		cells = new ArrayList<ArrayList<GridCell<T>>> ();
	}
	
	private void initGrid(int rows, int cols) {
		cells = new ArrayList<ArrayList<GridCell<T>>> ();
		for(int i=0;i<rows;i++){
			addRow();
			for(int j=0;j<cols;j++)
				addElementAt(i);
		}
	}
	
	public void add(int i,int j, T object){
		get(i,j).add(object);
	}
	
	public GridCell<T> get(GridPoint p) {
		return cells.get(p.getY()).get(p.getX());
	}
	
	public GridCell<T> get(int i,int j) {
		return cells.get(i).get(j);
	}
	
	protected void addRow(){
		cells.add(new ArrayList<GridCell<T>>());
	}
	
	private void addElementAt(int row){
		cells.get(row).add(new GridCell<T>());
	}
	
	protected void addElementAt(int row, T object){
		GridCell <T> cell = new GridCell<T>();
		cell.add(object);
		cells.get(row).add(cell);
	}
	
	public int getRows(){
		return cells.size();
	}
	
	public int getColumns(){
		return cells.get(0).size();
	}

	public ArrayList<T> getObjectsAt(int i, int j) {
		return get(i,j).getObjects();
	}
	
	/**
	 * Return Moore neighbourhood of gp, excluding cells over the boundaries of the grid
	 * */
	public Neighbourhood getNeighbourhood(GridPoint gp){
		Neighbourhood neighbourhood = new Neighbourhood();
		final int radius = 1;
		int row_gp = gp.getY();
		int col_gp = gp.getX();
		for(int row=row_gp-radius;row<=row_gp+radius;row++)
			for (int col=col_gp-radius;col<=col_gp+radius;col++)
				if (neighbourCondition(row,col))
					neighbourhood.add(new GridPoint(col,row));
		return neighbourhood;
	}
	
	public String toString(){
		String res="";
		for(int i=0;i<cells.size();i++){
			for(int j=0;j<cells.get(i).size();j++)
				res+=cells.get(i).get(j).toString()+" ";
			res+="\n";
		}
		return res;
	}
	
	protected boolean neighbourCondition(int row, int col){
		return row>=0 && col>=0 && col<getColumns() && row<getRows();
	}
	
	protected abstract void loadFromCSV(String fileName) throws IOException;
	
	public abstract void saveCSV(String path) throws IOException;

}
