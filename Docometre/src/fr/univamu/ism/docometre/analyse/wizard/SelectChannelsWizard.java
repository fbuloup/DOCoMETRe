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
		return selectedChannels[0] != null && selectedChannels[1] != null && selectedChannels[2] != null;
	}
	
	public Channel[] getSelectedChannels() {
		return selectedChannels;
	}

}
