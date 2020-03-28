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
package fr.univamu.ism.docometre.dacqsystems;

import fr.univamu.ism.docometre.DocometreMessages;

public final class ChannelProperties extends Property {
	
	public static final ChannelProperties ADD = new ChannelProperties("ChannelProperties.ADD", "", "", "");
	public static final ChannelProperties REMOVE = new ChannelProperties("ChannelProperties.REMOVE","", "", "");
	public static final ChannelProperties NAME = new ChannelProperties("ChannelProperties.NAME",DocometreMessages.Name_Label, DocometreMessages.Name_Tootip, "^[a-z|A-Z]+[0-9|a-z|A-Z|_]*$");
	public static final ChannelProperties SAMPLE_FREQUENCY = new ChannelProperties("ChannelProperties.SAMPLE_FREQUENCY",DocometreMessages.SampleFrequency_Label, DocometreMessages.SampleFrequency_Tooltip, "(^[+]?\\d*\\.?\\d*[1-9]+\\d*([eE][-+]?[0-9]+)?$)|(^[+]?[1-9]+\\d*\\.\\d*([eE][-+]?[0-9]+)?$)");
	public static final ChannelProperties CHANNEL_NUMBER = new ChannelProperties("ChannelProperties.CHANNEL_NUMBER",DocometreMessages.ChannelNumber_Label, DocometreMessages.ChannelNumber_Tooltip, "^[1-9]+[0-9]*$");
	public static final ChannelProperties TRANSFER_NUMBER = new ChannelProperties("ChannelProperties.TRANSFER_NUMBER",DocometreMessages.TransfertNumber_Label, DocometreMessages.TransfertNumber_Tooltip, "^[1-9]+[0-9]*$");
	public static final ChannelProperties BUFFER_SIZE = new ChannelProperties("ChannelProperties.BUFFER_SIZE",DocometreMessages.BufferSize_Label, DocometreMessages.BufferSize_Tooltip, "^[1-9]+[0-9]*$");
	public static final ChannelProperties TRANSFER = new ChannelProperties("ChannelProperties.TRANSFER",DocometreMessages.Transfert_Label, DocometreMessages.Transfert_Tooltip, "^(true|false)$", "true:false");
	public static final ChannelProperties AUTO_TRANSFER = new ChannelProperties("ChannelProperties.AUTO_TRANSFER",DocometreMessages.AutoTransfert_Label, DocometreMessages.AutoTransfert_Tooltip, "^(true|false)$", "true:false");
	public static final ChannelProperties RECORD = new ChannelProperties("ChannelProperties.RECORD",DocometreMessages.Record_Label, DocometreMessages.Record_Tooltip, "^(true|false)$", "true:false");
	
	public static void populateProperties(Channel channel){
		channel.setProperty(NAME, "name");
		channel.setProperty(SAMPLE_FREQUENCY, "1000");
		channel.setProperty(CHANNEL_NUMBER, "1");
		channel.setProperty(TRANSFER_NUMBER, "0");
		channel.setProperty(BUFFER_SIZE, "1000");
		channel.setProperty(TRANSFER, "false");
		channel.setProperty(AUTO_TRANSFER, "false");
		channel.setProperty(RECORD, "false");
	}

	public static void cloneChannel(Channel channel, Channel newChannel) {
		newChannel.setProperty(NAME, new String(channel.getProperty(NAME)));
		newChannel.setProperty(SAMPLE_FREQUENCY, new String(channel.getProperty(SAMPLE_FREQUENCY)));
		newChannel.setProperty(CHANNEL_NUMBER, new String(channel.getProperty(CHANNEL_NUMBER)));
		newChannel.setProperty(TRANSFER_NUMBER, new String(channel.getProperty(TRANSFER_NUMBER)));
		newChannel.setProperty(BUFFER_SIZE, new String(channel.getProperty(BUFFER_SIZE)));
		newChannel.setProperty(TRANSFER, new String(channel.getProperty(TRANSFER)));
		newChannel.setProperty(AUTO_TRANSFER, new String(channel.getProperty(AUTO_TRANSFER)));
		newChannel.setProperty(RECORD, new String(channel.getProperty(RECORD)));
	}
	
	private ChannelProperties(String key, String label, String tooltip, String regExp) {
		super(key, label, tooltip, regExp);
	}
	
	private ChannelProperties(String key, String label, String tooltip, String regExp, String availableValues) {
		super(key, label, tooltip, regExp, availableValues);
	}
	
}
