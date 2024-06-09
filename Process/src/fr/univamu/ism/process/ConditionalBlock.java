/*******************************************************************************
 * Copyright or © or Copr. Institut des Sciences du Mouvement 
 * (CNRS & Aix Marseille Université)
 * 
 * The DOCoMETER Software must be used with a real time data acquisition 
 * system marketed by ADwin (ADwin Pro and Gold, I and II) or an Arduino 
 * Uno. This software, created within the Institute of Movement Sciences, 
 * has been developed to facilitate their use by a "neophyte" public in the 
 * fields of industrial computing and electronics.  Students, researchers or 
 * engineers can configure this acquisition system in the best possible 
 * conditions so that it best meets their experimental needs. 
 * 
 * This software is governed by the CeCILL-B license under French law and
 * abiding by the rules of distribution of free software.  You can  use, 
 * modify and/ or redistribute the software under the terms of the CeCILL-B
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info". 
 * 
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability. 
 * 
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or 
 * data to be ensured and,  more generally, to use and operate it in the 
 * same conditions as regards security. 
 * 
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-B license and that you accept its terms.
 * 
 * Contributors:
 *  - Frank Buloup - frank.buloup@univ-amu.fr - initial API and implementation [25/03/2020]
 ******************************************************************************/
package fr.univamu.ism.process;

/**
 * Conditional bloc is the base abstract class for IF and DO blocs.
 * @author frank buloup
 *
 */
public abstract class ConditionalBlock extends Block {
	
	public static String LEFT_OPERAND = "LEFT_OPERAND";
	public static String OPERATOR = "OPERATOR";
	public static String RIGHT_OPERAND = "RIGHT_OPERAND";

	/**
	 * Serial Id version
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The left operand of the comparison.
	 */
	private String leftOperand;
	
	/**
	 * The operator of the comparison.
	 */
	private Operator operator = Operator.IS_EQUAL_TO;
	
	/**
	 * The right operand of the comparison.
	 */
	private String rightOperand;
	
	//Constructors
	public ConditionalBlock() {
	}
	
	public ConditionalBlock(Script script, String name) {
		super(script, name);
	}

	//Getters and setters
	public String getLeftOperand() {
		if(leftOperand == null) return "";
		return leftOperand;
	}

	public void setLeftOperand(String leftOperand) {
		this.leftOperand = leftOperand;
	}

	public Operator getOperator() {
		return operator;
	}

	public void setOperator(Operator operator) {
		this.operator = operator;
	}

	public String getRightOperand() {
		if(rightOperand == null) return "";
		return rightOperand;
	}

	public void setRightOperand(String rightOperand) {
		this.rightOperand = rightOperand;
	}
	
	protected ConditionalBlock clone(ConditionalBlock block) {
		block.setSizeAndLocation(getSizeAndLocation());
		block.setName(name);
		block.setLeftOperand(getLeftOperand());
		block.setOperator(getOperator());
		block.setRightOperand(getRightOperand());
		return block;
	}
	
}
