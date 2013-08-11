package eu.interedition.collatex.dekker.vectorspace;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import com.google.common.collect.Ranges;

/*
 * Class: VectorSpace
 * @Author: Ronald Haentjens Dekker
 * 
 * The idea is to build a multiple dimension vector space
 * with one dimension for each witness.
 * vectors represent phrase matches (=sequences of token matches)
 * between witnesses.
 * 
 * To test out the idea we first start with a 2d vector space
 */

public class VectorSpace {
	//Note: the direction of the vector is a constant,
	//so there is no need to store it.
	public class Vector {
		public final int[] startCoordinate;
		public int length;
		
		public Vector(int x, int y) {
		  this(1, x, y);
		}

    public Vector(int l, int x, int y) {
      this.startCoordinate = new int[] {x, y};
      this.length = l;
    }
    
    public Vector(int l, int x, int y, int z) {
      this.startCoordinate = new int[] {x, y, z};
      this.length = l;
    }

    @Override
    public String toString() {
      String result = String.format("V: Start coordinates: %s, length %d", Arrays.toString(startCoordinate), length);
      return result;
    }
    
    @Override
    public int hashCode() {
      return Objects.hash(startCoordinate, length);
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof Vector)) {
        return false;
      }
      Vector other = (Vector) obj;
      boolean result = Arrays.equals(other.startCoordinate, this.startCoordinate) && other.length == this.length;
      return result; 
    }

    public void extendLength() {
      this.length++;
    }

    //Note: this only checks whether the end coordinates of this 
    //and start coordinates of other are adjacent
    public boolean isAdjacent(Vector other) {
      int xDistance = Math.abs(startCoordinate[0]+length - other.startCoordinate[0]);
      int yDistance = Math.abs(startCoordinate[1]+length - other.startCoordinate[1]);
      return xDistance == 0 && yDistance == 0;
    }

    /*
     * This method checks whether one or more dimensions of this
     * vector and the other vector conflict with each other.
     */
    public boolean conflictsWith(Vector other) {
      // check x dimension
      Range<Integer> thisXRange = Ranges.closed(this.startCoordinate[0], this.startCoordinate[0]+this.length-1);
      Range<Integer> otherXRange = Ranges.closed(other.startCoordinate[0], other.startCoordinate[0]+other.length-1);
      if (thisXRange.encloses(otherXRange)) {
        return true;
      }
      // check y dimension
      Range<Integer> thisYRange = Ranges.closed(this.startCoordinate[1], this.startCoordinate[1]+this.length-1);
      Range<Integer> otherYRange = Ranges.closed(other.startCoordinate[1], other.startCoordinate[1]+other.length-1);
      if (thisYRange.encloses(otherYRange)) {
        return true;
      }
      return false;
    }

    // I am not sure that isParallel is the right name for this method
    // Note: this method can be more defensive!
    // for example check that the number of dimensions are equal
    public boolean isParallel(Vector other) {
      if (length!=other.length) {
        return false;
      }
      boolean parallel = true;
      for (int i=0; i < startCoordinate.length; i++) {
        int thisCoordinate = this.startCoordinate[i];
        int otherCoordinate = other.startCoordinate[i];
        parallel=parallel&&(thisCoordinate==0||otherCoordinate==0||thisCoordinate==otherCoordinate);
      }
      return parallel;
    }

    //NOTE: I could make this method more defensive
    //however that would mean doing stuff twice
    public Vector merge(Vector other) {
      int[] coordinates = new int[3];
      for (int i=0; i < startCoordinate.length; i++) {
        coordinates[i] = other.startCoordinate[i]==0 ? this.startCoordinate[i]:other.startCoordinate[i];
      }
      return new Vector(length, coordinates[0], coordinates[1], coordinates[2]);
    }

    public boolean isPresentIn(int dimension) {
      return startCoordinate[dimension]!=0;
    }
	}
	
	private List<Vector> vectors;
	
	public VectorSpace() {
		this.vectors = Lists.newArrayList();
	}
	
  public List<Vector> getVectors() {
    return vectors;
  }

  /*
   * Add a Vector of length 1 and n-dimensions to the vector space.
   * This method checks whether a parallel vector already exists
   * in the vector space. If yes, the two vectors will be merged.
   * This method checks whether there is an adjacent vector 
   * already exists in the space. If yes, add the new vector to it.
   * Returns the newly created vector object.
   */
  public Vector addVector(int... coordinates) {
    Vector vector = new Vector(1, coordinates[0], coordinates[1], coordinates[2]);
    Vector parallel = findParallelVector(vector);
    if (parallel!=null) {
      vector = vector.merge(parallel);
      vectors.remove(parallel);
    }
    Vector adjacentVector = findAdjacentVector(vector);
    if (adjacentVector!=null) {
      adjacentVector.extendLength();
      return adjacentVector;
    } 
    vectors.add(vector);
    return vector;
  }

	/*
	 * find an adjacent vector in the vector space to the other vector
	 * returns null if none is found 
	 */
  private Vector findAdjacentVector(Vector other) {
    for (Vector v : vectors) {
		  if (v.isAdjacent(other)) {
		    return v;
		  }
		}
    return null;
  }

  /*
   * find a parallel vector in the vector space to the other vector
   * returns null if none is found 
   */
  private Vector findParallelVector(Vector other) {
    for (Vector v : vectors) {
      if (v.isParallel(other)) {
        return v;
      }
    }
    return null;
  }

  public void remove(Vector v) {
    boolean removed = vectors.remove(v);
    if (!removed) {
      throw new RuntimeException("Vector "+v+" not present in vector space");
    }
  }
}