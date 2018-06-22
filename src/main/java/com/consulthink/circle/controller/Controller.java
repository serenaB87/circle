package com.consulthink.circle.controller;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Utils;
import org.bitcoinj.wallet.DeterministicSeed;
import org.gmagnotta.log.LogEventWriter;
import org.gmagnotta.log.LogLevel;
import org.gmagnotta.log.impl.filesystem.FileSystemLogEventWriter;
import org.gmagnotta.log.impl.filesystem.FileSystemLogStore;
import org.gmagnotta.log.impl.system.MarkerAwareConsoleLogEventWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.consulthink.circle.CircleSettings;
import com.consulthink.circle.model.CircleObj;
import com.consulthink.circle.model.MessageObj;
import com.uniquid.connector.Connector;
import com.uniquid.connector.impl.MQTTConnector;
import com.uniquid.core.impl.UniquidSimplifier;
import com.uniquid.messages.AnnounceMessage;
import com.uniquid.node.UniquidNodeState;
import com.uniquid.node.impl.UniquidNodeImpl;
import com.uniquid.node.listeners.EmptyUniquidNodeEventListener;
import com.uniquid.params.UniquidRegTest;
import com.uniquid.register.RegisterFactory;
import com.uniquid.register.circle.CircleChannel;
import com.uniquid.register.guest.GuestChannel;
import com.uniquid.register.impl.sql.SQLiteRegisterFactory;
import com.uniquid.register.provider.ProviderChannel;
import com.uniquid.register.user.UserChannel;
import com.uniquid.settings.exception.SettingValidationException;
import com.uniquid.settings.exception.UnknownSettingException;
import com.uniquid.userclient.impl.MQTTUserClient;
import com.uniquid.utils.BackupData;
import com.uniquid.utils.SeedUtils;


@RestController
@RequestMapping( path="/circles")
public class Controller {

	private static final Logger LOGGER = LoggerFactory.getLogger(Controller.class.getName());
	private static final String CONSOLE = "CONSOLE";
	
	private static final Marker MARKER = MarkerFactory.getMarker(CONSOLE);
	
	private static final String APPCONFIG_PROPERTIES = "/appconfig.properties";
	
	private static final String DEBUG = "DEBUG";
	
	private CircleSettings getAppSettings(){
		org.gmagnotta.log.LogEventCollector.getInstance().setLogLevelThreshold(LogLevel.INFO);
		
		String debug = System.getProperty(DEBUG);
		
		if (debug != null) {
			org.gmagnotta.log.LogEventCollector.getInstance().setLogLevelThreshold(LogLevel.TRACE);
		}

		LOGGER.info(MARKER, "Starting circles...");

		FileSystemLogStore fileSystemLogStore = new FileSystemLogStore(1 * 1024 * 1024, 3, new File("."));
		
		FileSystemLogEventWriter fs;
		CircleSettings appSettings = null;
		
		try {
			fs = new FileSystemLogEventWriter(fileSystemLogStore);
		
			org.gmagnotta.log.LogEventCollector.getInstance().addLogEventWriter(fs);
		
			MarkerAwareConsoleLogEventWriter markerAwareConsoleLogEventWriter = new MarkerAwareConsoleLogEventWriter(CONSOLE);
		
			org.gmagnotta.log.LogEventCollector.getInstance().addLogEventWriter(markerAwareConsoleLogEventWriter);
		
			// Read configuration properties
			InputStream inputStream = null;
	
				
			// if the user did not pass properties file, then we use our default one (inside the jar)
			inputStream = Controller.class.getResourceAsStream(APPCONFIG_PROPERTIES);	
			
			Properties properties = new Properties();
			properties.load(inputStream);

		// close input stream
			inputStream.close();

			appSettings = new CircleSettings(properties);
		} catch (SettingValidationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnknownSettingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		return appSettings;
	}
	
	private RegisterFactory getRegisterFactory() throws Exception{
		
		CircleSettings appSettings = this.getAppSettings();
		//
		// 1 Create Register Factory: we choose the SQLiteRegisterFactory implementation.
		//
		RegisterFactory registerFactory = new SQLiteRegisterFactory(appSettings.getDBUrl());

		return registerFactory;
	}

	@RequestMapping(path = "/checkExist/{masterId}", method = RequestMethod.POST)
    @ResponseBody
    public MessageObj checkExist(@PathVariable(name="masterId") String masterId) {
		RegisterFactory register;
		MessageObj result = new MessageObj();
		try {
			register = this.getRegisterFactory();
			Boolean check = register.getCircleRegister().checkExists(masterId);
			
			if(check){
				result.setCode(200);
				result.setMessage("OK");
			}else{
				result.setCode(404);
				result.setMessage("Circle not found");
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return result;
    }
	
	@RequestMapping(path = "/getCircle/{masterId}", method = RequestMethod.POST)
    @ResponseBody
    public CircleObj getCircleByMasterId(@PathVariable(name="masterId") String masterId) {
		RegisterFactory register;
		CircleObj result = new CircleObj();
		CircleChannel circle = new CircleChannel();
		List<GuestChannel> guests = new ArrayList<GuestChannel>();
		
		try {
			register = this.getRegisterFactory();
			circle = register.getCircleRegister().getCircle(masterId);
			guests = register.getGuestsRegister().getGuestsByCircleName(masterId);
			result.setId(circle.getId());
			result.setName(circle.getName());
			result.setGuests(guests);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
        return result;
    }

	@RequestMapping(path = "/createCircle/{masterId}", method = RequestMethod.POST)
    @ResponseBody
    public MessageObj createCircleByMasterId(@PathVariable(name="masterId") String masterId) {
		RegisterFactory register;
		MessageObj result = new MessageObj();
		
		try {
			register = this.getRegisterFactory();
			register.getCircleRegister().insertCircle(masterId);
//			this.circleSettings(masterId);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
        return result;
    }
	
//	private void circleSettings(String masterId) throws Exception{
//
//		final CircleSettings appSettings = this.getAppSettings();
//		
//		// Read network parameters
//		NetworkParameters networkParameters = appSettings.getNetworkParameters();
//
//		// Read provider wallet
//		File providerWalletFile = appSettings.getProviderWalletFile();
//
//		// Read wallet file
//		File userWalletFile = appSettings.getUserWalletFile();
//		
//		// Read chain file
//		File chainFile = appSettings.getChainFile();
//		
//		// Read chain file
//		File userChainFile = appSettings.getUserChainFile();
//		
//		// Machine name
//		String machineName = masterId+"_circle";
//		
//		// Seed backup file
//		File seedFile = appSettings.getSeedFile();
//		
//		// Retrieve list of blockchain peers
//		String bcpeers = appSettings.getBlockChainPeers();
//		
//		RegisterFactory registerFactory = this.getRegisterFactory();
//		
//		// Tell library to use those peers
//		
//		UniquidRegTest.get().overridePeers(bcpeers);
//		
//		//
//		// 2 start to construct an UniquidNode...
//		//
//		final UniquidNodeImpl uniquidNode;
//		
//		// ... if the seed file exists then we use the SeedUtils to open it and decrypt its content: we can extract the
//		// mnemonic string, creationtime and name to restore the node; otherwise we create a new node initialized with a
//		// random seed and then we use a SeedUtils to perform an encrypted backup of the seed and other properties
//		if (seedFile.exists() && !seedFile.isDirectory()) {
//			
//			// create a SeedUtils (the wrapper that is able to load/read/decrypt the seed file)
//			SeedUtils<BackupData> seedUtils = new SeedUtils<BackupData>(seedFile);
//			
//			// decrypt the content with the password read from the application setting properties
//			BackupData readData = new BackupData(); 
//
//			seedUtils.readData(appSettings.getSeedPassword(), readData);
//
//			// fetch mnemonic string
//			final String mnemonic = readData.getMnemonic();
//
//			// fetch creation time
//			final long creationTime = readData.getCreationTime();
//			
//			machineName = readData.getName();
//			
//			// now we build an UniquidNode with the data read from seed file: we choose the UniquidNodeImpl
//			// implementation
//			@SuppressWarnings("rawtypes")
//			UniquidNodeImpl.UniquidNodeBuilder builder = new UniquidNodeImpl.UniquidNodeBuilder();
//			
//			builder.setNetworkParameters(networkParameters).
//					setProviderFile(providerWalletFile).
//					setUserFile(userWalletFile).
//					setProviderChainFile(chainFile).
//					setUserChainFile(userChainFile).
//					setRegisterFactory(registerFactory).
//					setNodeName(machineName);
//			
//			uniquidNode = builder.buildFromMnemonic(mnemonic, creationTime);
//			
//		} else {
//		
//			// We create a builder with specified settings
//			@SuppressWarnings("rawtypes")
//			UniquidNodeImpl.UniquidNodeBuilder builder = new UniquidNodeImpl.UniquidNodeBuilder();
//					builder.setNetworkParameters(networkParameters)
//					.setProviderFile(providerWalletFile)
//					.setUserFile(userWalletFile)
//					.setProviderChainFile(chainFile)
//					.setUserChainFile(userChainFile)
//					.setRegisterFactory(registerFactory)
//					.setNodeName(machineName);
//			
//			// ask the builder to create a node with a random seed
//			uniquidNode = builder.build();
//			
//			// Now we fetch from the builder the DeterministicSeed that allow us to export mnemonics and creationtime
//			DeterministicSeed seed = uniquidNode.getDeterministicSeed();
//			
//			// we save the creation time
//			long creationTime = seed.getCreationTimeSeconds();
//			
//			// we save mnemonics
//			String mnemonics = Utils.join(seed.getMnemonicCode());
//			
//			// we prepare the data to save for seedUtils
//			BackupData backupData = new BackupData();
//			backupData.setMnemonic(mnemonics);
//			backupData.setCreationTime(creationTime);
//			backupData.setName(machineName);
//			
//			// we construct a seedutils
//			SeedUtils<BackupData> seedUtils = new SeedUtils<BackupData>(seedFile);
//			
//			// now backup mnemonics encrypted on disk
//			seedUtils.saveData(backupData, appSettings.getSeedPassword());
//		
//		}
//		
//		final String senderTopic = machineName;
//		//
//		// 2 ...we finished to build an UniquidNode
//		// 
//		
//		// Here we register a callback on the uniquidNode that allow us to be triggered when some interesting events happens
//		// Currently we are only interested in receiving the onNodeStateChange() event. The other methods are present
//		// because we decided to use an anonymous inner class.
//		uniquidNode.addUniquidNodeEventListener(new EmptyUniquidNodeEventListener() {
//			
//			@Override
//			public void onNodeStateChange(UniquidNodeState arg0) {
//
//				// Register an handler that allow to send an imprinting message to the imprinter
//				try {
//					
//					LOGGER.info(MARKER, "circle new state: " + arg0);
//
//					// If the node is ready to be imprinted...
//					if (UniquidNodeState.IMPRINTING.equals(arg0)) {
//
//						// Create a MQTTClient pointing to the broker on the UID/announce topic and specify
//						// 0 timeout: we don't want a response.
//						final MQTTUserClient userClient = new MQTTUserClient(appSettings.getMQTTBroker(), appSettings.getAnnounceTopic(), 0, senderTopic);
//						
//						AnnounceMessage announceMessage = new AnnounceMessage();
//						announceMessage.setName(uniquidNode.getNodeName());
//						announceMessage.setPubKey(uniquidNode.getPublicKey());
//						
//						LOGGER.info(MARKER, "Announcing circle on MQTT to imprinter");
//						
//						// send the request.  The server will not reply (but will do an imprint on blockchain)
//						userClient.send(announceMessage);
//						
//					}
//
//				} catch (Exception ex) {
//					// expected! the server will not reply
//				}
//			}
//			
//			@Override
//			public void onProviderContractCreated(ProviderChannel providerChannel) {
//				LOGGER.info(MARKER, "Created Provider Contract: " + providerChannel);
//			}
//
//			@Override
//			public void onProviderContractRevoked(ProviderChannel providerChannel) {
//				LOGGER.info(MARKER, "Revoked Provider Contract: " + providerChannel);
//			}
//
//			@Override
//			public void onUserContractCreated(UserChannel userChannel) {
//				LOGGER.info(MARKER, "Created User Contract: " + userChannel);
//			}
//
//			@Override
//			public void onUserContractRevoked(UserChannel userChannel) {
//				LOGGER.info(MARKER, "Revoked User Contract: " + userChannel);
//			}
//
//		});
//		
//		//
//		// 3 Create connector: we choose the MQTTConnector implementation
//		//
//		final Connector mqttProviderConnector = new MQTTConnector.Builder()
//				.set_broker(appSettings.getMQTTBroker())
//				.set_topic(machineName)
//				.build();
//		
//		// 
//		// 4 Create UniquidSimplifier that wraps registerFactory, connector and uniquidnode
//		final UniquidSimplifier simplifier = new UniquidSimplifier(registerFactory, mqttProviderConnector, uniquidNode);
//		
//		// 5 Register custom functions on slot 34, 35, 36
//		simplifier.addFunction(new TankFunction(), 34);
//		simplifier.addFunction(new InputFaucetFunction(), 35);
//		simplifier.addFunction(new OutputFaucetFunction(), 36);
//		
//		LOGGER.info(MARKER, "Starting Uniquid library with node: " + machineName);
//		
//		// Set static values for Tank singleton
//		Tank.mqttbroker = appSettings.getMQTTBroker();
//		Tank.tankname = machineName;
//		
//		//
//		// 6 start Uniquid core library: this will init the node, sync on blockchain, and use the provided
//		// registerFactory to interact with the persistence layer
//		simplifier.start();
//		
//		// Register shutdown hook
//		Runtime.getRuntime().addShutdownHook(new Thread() {
//			
//			public void run() {
//
//				LOGGER.info(MARKER, "Terminating circle");
//				try {
//
//					// tell the library to shutdown and close all opened resources
//					simplifier.shutdown();
//
//					// explicitly stop logging
//					for (LogEventWriter writer : org.gmagnotta.log.LogEventCollector.getInstance().getLogEventWriters()) {
//
//						writer.stop();
//
//					}
//
//					org.gmagnotta.log.LogEventCollector.getInstance().stop();
//
//				} catch (Exception ex) {
//
//					LOGGER.error("Exception while terminating circle", ex);
//
//				}
//			}
//		});
//		
//		LOGGER.info(MARKER, "Circle ready");
//	}
}