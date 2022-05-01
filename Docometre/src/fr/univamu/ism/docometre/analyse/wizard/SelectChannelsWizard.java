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
package fr.univamu.ism.docometre.analyse.wizard;

import org.eclipse.jface.wizard.Wizard;

import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.analyse.datamodel.Channel;

public class SelectChannelsWizard extends Wizard {
	
	public static enum ChannelsNumber {
		ONE,
		TWO,
		THREE
	}
	
	private ChannelsNumber channelsNumber;
	private SelectChannelsWizardPage firstChannelPage;
	private SelectChannelsWizardPage secondChannelPage;
	private SelectChannelsWizardPage thirdChannelPage;
	private Channel[] availableChannels;
	private Channel[] selectedChannels;
	
	public SelectChannelsWizard(ChannelsNumber channelsNumber, Channel[] channels) {
		this.channelsNumber = channelsNumber;
		this.availableChannels = channels;
	}
	
	@Override
	public String getWindowTitle() {
		return DocometreMessages.AxisSelectionDialogTitle;
	}
	
	@Override
	public void addPages() {
		firstChannelPage = new SelectChannelsWizardPage("SelectChannelsWizardPage_First", availableChannels, DocometreMessages.XAxisSelectionDialogSubTitle, DocometreMessages.AxisSelectionDialogMessage);
		selectedChannels = new Channel[1];
		super.addPage(firstChannelPage);
		if(channelsNumber == ChannelsNumber.TWO || channelsNumber == ChannelsNumber.THREE) {
			secondChannelPage = new SelectChannelsWizardPage("SelectChannelsWizardPage_Second", availableChannels, DocometreMessages.YAxisSelectionDialogSubTitle, DocometreMessages.AxisSelectionDialogMessage);
			selectedChannels = new Channel[2];
			super.addPage(secondChannelPage);
		}
		if(channelsNumber == ChannelsNumber.THREE) {
			thirdChannelPage = new SelectChannelsWizardPage("SelectChannelsWizardPage_Third", availableChannels, DocometreMessages.ZAxisSelectionDialogSubTitle, DocometreMessages.AxisSelectionDialogMessage);
			selectedChannels = new Channel[3];
			super.addPage(thirdChannelPage);
		}
	}

	@Override
	public boolean performFinish() {
		selectedChannels[0] = firstChannelPage.getChannel();
		if(channelsNumber == ChannelsNumber.TWO || channelsNumber == ChannelsNumber.THREE) selectedChannels[1] = secondChannelPage.getChannel();
		if(channelsNumber == ChannelsNumber.THREE) selectedChannels[2] = thirdChannelPage.getChannel();
		if(channelsNumber == ChannelsNumber.THREE) return selectedChannels[0] != null && selectedChannels[1] != null && selectedChannels[2] != null;
		if(channelsNumber == ChannelsNumber.TWO) return selectedChannels[0] != null && selectedChannels[1] != null;
		return selectedChannels[0] != null;
	}
	
	public Channel[] getSelectedChannels() {
		return selectedChannels;
	}

}
