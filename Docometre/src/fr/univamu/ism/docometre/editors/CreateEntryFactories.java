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
package fr.univamu.ism.docometre.editors;

import org.eclipse.gef.requests.CreationFactory;
import org.eclipse.gef.requests.SimpleFactory;

import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.process.Comment;
import fr.univamu.ism.process.DoBlock;
import fr.univamu.ism.process.Function;
import fr.univamu.ism.process.IfBlock;
import fr.univamu.ism.process.Script;

public final class CreateEntryFactories {
	
	public static final CreationFactory getIfBlockFactory(Script script) {
		SimpleFactory<IfBlock> blockFactory = new SimpleFactory<IfBlock>(IfBlock.class) {
			@Override
			public IfBlock getNewObject() {
				IfBlock block = new IfBlock(script, DocometreMessages.NewIfBlockName);
				return block;
			}
		};
		return blockFactory;
	}
	
	public static final CreationFactory getDoBlockFactory(Script script) {
		SimpleFactory<DoBlock> blockFactory = new SimpleFactory<DoBlock>(DoBlock.class) {
			@Override
			public DoBlock getNewObject() {
				DoBlock block = new DoBlock(script, DocometreMessages.NewDoBlockName);
				return block;
			}
		};
		return blockFactory;
	}
	
	public static final CreationFactory getFunctionFactory(Script script) {
		SimpleFactory<Function> blockFactory = new SimpleFactory<Function>(Function.class) {
			@Override
			public Function getNewObject() {
				Function block = new Function(script, DocometreMessages.NewFunctionName);
//				block.setToolTip(DocometreMessages.NewFunctionTooltip);
				return block;
			}
		};
		return blockFactory;
	}
	
	public static final CreationFactory getCommentBlockFactory(Script script) {
		SimpleFactory<Comment> blockFactory = new SimpleFactory<Comment>(Comment.class) {
			@Override
			public Comment getNewObject() {
				Comment block = new Comment(script, DocometreMessages.NewCommentName);
				return block;
			}
		};
		return blockFactory;
	}

}
