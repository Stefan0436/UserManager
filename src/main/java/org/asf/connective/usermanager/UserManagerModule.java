package org.asf.connective.usermanager;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.stream.Stream;

import org.asf.aos.util.service.extra.slib.util.ArrayUtil;
import org.asf.connective.usermanager.api.AuthSecureStorage;
import org.asf.connective.usermanager.api.IAuthenticationBackend;
import org.asf.connective.usermanager.api.IUserManagerCommand;
import org.asf.connective.usermanager.configs.physical.ActivationKeyConfig;
import org.asf.connective.usermanager.configs.physical.ProductKeyConfig;
import org.asf.connective.usermanager.implementation.DefaultAdminPanel;
import org.asf.connective.usermanager.implementation.DefaultAuthFrontend;
import org.asf.connective.usermanager.implementation.DefaultAuthSecureStorage;
import org.asf.connective.usermanager.implementation.html.HTMLFrontendLogin;
import org.asf.cyan.api.common.CYAN_COMPONENT;
import org.asf.rats.Memory;
import org.asf.rats.ModuleBasedConfiguration;

@CYAN_COMPONENT
public class UserManagerModule extends UserManagerModificationManager {

	private static HashMap<String, AuthSecureStorage> userStorage = new HashMap<String, AuthSecureStorage>();
	private static SecureRandom secureRandom = new SecureRandom();

	private static HashMap<String, String> authServices;

	private static int productKeySegments = 5;
	private static int productKeySegmentLength = 5;

	private static int activationKeySegments = 10;
	private static int activationKeySegmentLength = 7;

	private static int cancelKeySegments = 10;
	private static int cancelKeySegmentLength = 7;

	private static IAuthenticationBackend backend = null;
	private static HashMap<String, String> configuration = new HashMap<String, String>();
	private static boolean hasConfigChanged = false;

	private static String[] allowedGroups = null;
	private static String[] productGroups = null;

	private static ArrayList<ActivationKeyConfig> activationKeys = new ArrayList<ActivationKeyConfig>();

	private static File productKeyDir = null;
	private static File activationDir = null;

	private static File userDataDir = null;

	private static Thread keyCheckerThread = new Thread(() -> {
		while (true) {

			for (String group : allowedGroups.clone()) {
				if (isProductGroup(group)) {
					for (String key : getValidProductKeys(group)) {
						try {
							if (!getProductKey(group, key).isStillValid()) {
								getProductKeyFile(group, key).delete();
							}
						} catch (IOException e) {
						}
					}
				}
			}

			for (ActivationKeyConfig key :

			getActivationKeys()) {
				try {
					Date expiry = parseDateString(key.expiryDate);
					if (new Date().after(expiry)) {
						removeActivationKey(key, false);
						activationKeys.remove(key);
					}
				} catch (ParseException | IOException e) {
				}
			}

			try {
				Thread.sleep(60 * (60 * 1000));
			} catch (InterruptedException e) {
				break;
			}
		}
	}, "UserManager Activation Expiry Thread");

	private static IUserManagerCommand[] commands = new IUserManagerCommand[0];

	public static boolean isValidService(String service) {
		return authServices.containsKey(service);
	}

	public static String getService(String service) {
		return authServices.get(service);
	}

	public static File getProductKeyFile(String group, String productKey) {
		return new File(productKeyDir, "unused-keys/" + group + "/" + productKey + ".key");
	}

	public static File getSubscribedProductKeyFile(String group, String productKey) {
		return new File(productKeyDir, "active-subscriptions/" + group + "/" + productKey + ".key");
	}

	public static ProductKeyConfig getProductKey(String group, String productKey) throws IOException {
		return new ProductKeyConfig().readAll(Files.readString(getProductKeyFile(group, productKey).toPath()))
				.setInfo(productKey);
	}

	public static IUserManagerCommand[] getCommands() {
		return commands;
	}

	public static IAuthenticationBackend getAuthBackend() {
		return backend;
	}

	public static String[] getAllowedGroups() {
		return allowedGroups;
	}

	public static synchronized ActivationKeyConfig[] getActivationKeys() {
		return activationKeys.toArray(t -> new ActivationKeyConfig[t]);
	}

	public static synchronized ActivationKeyConfig getActivationKey(String key) {
		ArrayList<ActivationKeyConfig> keys = new ArrayList<ActivationKeyConfig>(activationKeys);

		if (!keys.stream().anyMatch(t -> t.key.equals(key)))
			return null;

		return keys.stream().filter(t -> t.key.equals(key)).findFirst().get();
	}

	public static synchronized ActivationKeyConfig getActivationKeyByCancelKey(String key) {
		ArrayList<ActivationKeyConfig> keys = new ArrayList<ActivationKeyConfig>(activationKeys);

		if (!keys.stream().anyMatch(t -> t.cancelKey.equals(key)))
			return null;

		return keys.stream().filter(t -> t.cancelKey.equals(key)).findFirst().get();
	}

	public static synchronized File getActivationKeyFile(ActivationKeyConfig key) {
		return new File(activationDir, key.group + "/" + key.key + ".ccfg");
	}

	public static void removeActivationKey(ActivationKeyConfig key, boolean activated) throws IOException {
		getActivationKeyFile(key).delete();
		activationKeys.remove(key);

		if (key.productKey != null) {
			File keyFile = getProductKeyFile(key.group, key.productKey.key);

			if (activated && key.productKey.remainingUses != -1) {
				key.productKey.remainingUses--;
			}
			key.productKey.key = null;

			if (key.productKey.remainingUses != 0)
				Files.writeString(keyFile.toPath(), key.productKey.toString());
		}
	}

	public static ProductKeyConfig generateUnusedProductKey(String group, int maxUses, int expiryDays)
			throws IOException {
		File productKeys = new File(productKeyDir, "unused-keys/" + group);
		File subscriptions = new File(productKeyDir, "active-subscriptions/" + group);

		if (!productKeys.exists())
			productKeys.mkdirs();
		if (!subscriptions.exists())
			subscriptions.mkdirs();

		ArrayList<String> keys = new ArrayList<String>();
		for (File keyFile : productKeys.listFiles((file) -> !file.isDirectory() && file.getName().endsWith(".key"))) {
			keys.add(keyFile.getName().toUpperCase().substring(0, keyFile.getName().lastIndexOf(".key")));
		}
		for (File keyFile : subscriptions.listFiles((file) -> !file.isDirectory() && file.getName().endsWith(".key"))) {
			keys.add(keyFile.getName().toUpperCase().substring(0, keyFile.getName().lastIndexOf(".key")));
		}
		for (ActivationKeyConfig conf : getActivationKeys()) {
			if (conf.productKey != null && !keys.contains(conf.productKey.key))
				keys.add(conf.productKey.key);
		}

		String key = generateKey(productKeySegments, productKeySegmentLength);
		while (keys.contains(key)) {
			key = generateKey(productKeySegments, productKeySegmentLength);
		}

		ProductKeyConfig conf = new ProductKeyConfig(expiryDays);
		conf.remainingUses = maxUses;
		Files.writeString(getProductKeyFile(group, key).toPath(), conf.toString());
		conf.key = key;

		return conf;
	}

	public static String getDateString(Date date) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-YYYY");
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		return dateFormat.format(date);
	}

	public static Date parseDateString(String date) throws ParseException {
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		return dateFormat.parse(date);
	}

	public static ActivationKeyConfig generateUnusedActivationKey(String group, String username, char[] password,
			String activationKey) throws IOException {

		ArrayList<String> keys = new ArrayList<String>();
		ArrayList<String> cancelkeys = new ArrayList<String>();
		for (ActivationKeyConfig key : getActivationKeys()) {
			keys.add(key.key);
			cancelkeys.add(key.cancelKey);
		}

		String key = generateKey(activationKeySegments, activationKeySegmentLength);
		while (keys.contains(key)) {
			key = generateKey(activationKeySegments, activationKeySegmentLength);
		}

		String cancelKey = generateKey(cancelKeySegments, cancelKeySegmentLength);
		while (cancelkeys.contains(key)) {
			cancelKey = generateKey(cancelKeySegments, cancelKeySegmentLength);
		}

		ActivationKeyConfig aKey = new ActivationKeyConfig();
		aKey.cancelKey = cancelKey;
		aKey.key = key;
		aKey.group = group;
		aKey.username = username;
		aKey.password = Base64.getEncoder().encodeToString(new String(password).getBytes());

		if (activationKey != null) {
			aKey.productKey = getProductKey(group, activationKey);
		}

		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, Integer.valueOf(configuration.get("activation-period")));
		aKey.expiryDate = getDateString(cal.getTime());

		File keyDir = new File(activationDir, group);
		if (!keyDir.exists())
			keyDir.mkdirs();

		aKey.file = new File(keyDir, aKey.key + ".ccfg");
		Files.writeString(aKey.file.toPath(), aKey.toString());

		activationKeys.add(aKey);

		return aKey;
	}

	private static char[] allowedChars = ArrayUtil.castWrapperArrayToPrimitive(ArrayUtil.buildArray('0',
			ArrayUtil.rangingNumeric('0', '9', false, true), ArrayUtil.rangingNumeric('A', 'Z', false, true)),
			new char[0]);

	protected static String generateKey(int segments, int length) {
		String key = "";
		for (int i = 0; i < segments; i++) {
			if (!key.isEmpty())
				key += "-";
			for (int i2 = 0; i2 < length; i2++) {
				key += allowedChars[secureRandom.nextInt(allowedChars.length)];
			}
		}
		return key;
	}

	public static String[] getValidProductKeys(String group) {
		File productKeys = new File(productKeyDir, "unused-keys/" + group);
		File subscriptions = new File(productKeyDir, "active-subscriptions/" + group);

		if (!productKeys.exists())
			productKeys.mkdirs();
		if (!subscriptions.exists())
			subscriptions.mkdirs();

		ArrayList<String> keys = new ArrayList<String>();

		for (File keyFile : productKeys.listFiles((file) -> !file.isDirectory() && file.getName().endsWith(".key"))) {
			keys.add(keyFile.getName().toUpperCase().substring(0, keyFile.getName().lastIndexOf(".key")));
		}

		return keys.toArray(t -> new String[t]);
	}

	public static boolean isProductGroup(String group) {
		return Stream.of(productGroups).anyMatch(t -> t.equals(group));
	}

	public static String getBase() {
		String base = configuration.get("base-uri");
		while (base.startsWith("/"))
			base = base.substring(1);
		while (base.endsWith("/"))
			base = base.substring(base.length() - 1);
		base = "/" + base;
		return base;
	}

	@Override
	protected String moduleId() {
		return "UserManager";
	}

	static {
		DefaultAuthSecureStorage.assign();
		DefaultAdminPanel.assign();

		configuration.put("auth-secure-storage", "storage/users");
		configuration.put("activated-users-storage", "cache/usermanager/activated-users");

		configuration.put("product-key-segments", "5");
		configuration.put("product-key-segment-length", "5");

		configuration.put("activation-key-segments", "10");
		configuration.put("activation-key-segment-length", "7");

		configuration.put("cancel-key-segments", "10");
		configuration.put("cancel-key-segment-length", "7");

		configuration.put("authentication-backend", "credtool");

		configuration.put("product-key-data", "cache/usermanager/product-keys");
		configuration.put("user-activation-data", "cache/usermanager/user-activation");

		configuration.put("base-uri", "/users");

		configuration.put("authenticate", "/authenticate");
		configuration.put("create-user", "/create");
		configuration.put("cancel-activation", "/cancel");
		configuration.put("update-user", "/update");
		configuration.put("remove-user", "/delete");
		configuration.put("activate-user", "/activate");

		configuration.put("admin-group", "server");

		configuration.put("admin-commands", "/admin");
		configuration.put("auth-frontend", "modular");
//		configuration.put("admin-del-productkeys", "/admin/del/productkey"); // TODO
//
//		configuration.put("admin-create-user", "/admin/create/user"); // TODO
//
//		configuration.put("admin-add-allowed-group", "/admin/add-allowed/group"); // TODO
//		configuration.put("admin-remove-allowed-group", "/admin/remove-allowed/group"); // TODO
//
//		configuration.put("admin-add-product-group", "/admin/add-product/group"); // TODO
//		configuration.put("admin-remove-product-group", "/admin/remove-product/group"); // TODO
//
//		configuration.put("admin-del-user", "/admin/del/user"); // TODO
//		configuration.put("admin-del-group", "/admin/del/group"); // TODO
//		configuration.put("admin-update-user", "/admin/update/user"); // TODO
//		configuration.put("admin-reload-server", "/admin/reload/configs"); // TODO
//
		configuration.put("allowed-groups", "users");
		configuration.put("product-groups", "premium maven git");

		configuration.put("activation-period", "30");

		configuration.put("mailcommand", "/usr/bin/sendmail");
		configuration.put("mailarguments", "%recipient%");

		configuration.put("mail-sender", "noreply@localhost");
		configuration.put("mail-template-activation", "\n" + "From: %sender%\n" + "Subject: Account Activation Key\n"
				+ "Dear user, at %date% you have requested a %group% account.\n"
				+ "Your username is %username%, in this mail, you will find an account activation key.\n\n"
				+ "You can cancel the request at any time by using the cancel key\n\n" + "Cancel key: %cancelkey%\n"
				+ "Activation key: %activationkey%\n\n" + "NOTE: they keys are only valid until %expiry-date%.\n");
	}

	protected static void initComponent() throws IOException {
		assign(new UserManagerModule());
		UserManagerModule.start();
	}

	@Override
	protected void startModule() {
		Memory.getInstance().getOrCreate("bootstrap.call").<Runnable>append(() -> {
			readConfig();
			keyCheckerThread.start();
		});
		Memory.getInstance().getOrCreate("bootstrap.reload").<Runnable>append(() -> readConfig());
	}

	private void readConfig() {
		hasConfigChanged = false;

		ModuleBasedConfiguration<?> config = Memory.getInstance().get("memory.modules.shared.config")
				.getValue(ModuleBasedConfiguration.class);

		HashMap<String, String> category = config.modules.getOrDefault(moduleId(), new HashMap<String, String>());

		if (!config.modules.containsKey("UserManager-AuthServices")) {
			config.modules.put("UserManager-AuthServices", new HashMap<String, String>());
			hasConfigChanged = true;
		}
		if (!config.modules.containsKey(moduleId())) {
			category.putAll(configuration);
			hasConfigChanged = true;
		} else {
			configuration.forEach((key, value) -> {
				if (!category.containsKey(key)) {
					hasConfigChanged = true;
					category.put(key, value);
				} else {
					configuration.put(key, category.get(key));
				}

			});
		}

		config.modules.put(moduleId(), category);
		if (hasConfigChanged) {
			try {
				config.writeAll();
			} catch (IOException e) {
				error("Config saving failed!", e);
			}
		}

		authServices = config.modules.get("UserManager-AuthServices");

		ArrayList<String> groups = new ArrayList<String>();
		groups.addAll(parseCommand(configuration.get("allowed-groups")));
		groups.addAll(parseCommand(configuration.get("product-groups")));
		allowedGroups = groups.toArray(t -> new String[t]);

		productGroups = parseCommand(configuration.get("product-groups")).toArray(t -> new String[t]);

		Class<IUserManagerCommand>[] classes = findClasses(getMainImplementation(), IUserManagerCommand.class);
		commands = new IUserManagerCommand[classes.length];
		int i = 0;
		for (Class<IUserManagerCommand> impl : classes) {
			if (!impl.isInterface()) {
				try {
					IUserManagerCommand inst = impl.getConstructor().newInstance();
					commands[i++] = inst;
				} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
						| InvocationTargetException | NoSuchMethodException | SecurityException e) {
					throw new RuntimeException(e);
				}
			}
		}

		productKeyDir = new File(configuration.get("product-key-data"));
		activationDir = new File(configuration.get("user-activation-data"));
		userDataDir = new File(configuration.get("activated-users-storage"));

		activationKeys.clear();

		for (String group : groups) {
			File keyDir = new File(activationDir, group);
			if (keyDir.exists()) {
				for (File keyFile : keyDir.listFiles(t -> !t.isDirectory() && t.getName().endsWith(".ccfg"))) {
					try {
						activationKeys.add(new ActivationKeyConfig().readAll(Files.readString(keyFile.toPath()))
								.setInfo(group, keyFile));
					} catch (IOException e) {
						error("Failed to load activation keyfile " + keyFile.getName(), e);
					}
				}
			}
		}

		if (!productKeyDir.exists())
			productKeyDir.mkdirs();
		if (!activationDir.exists())
			activationDir.mkdirs();
		if (!userDataDir.exists())
			userDataDir.mkdirs();

		productKeySegments = Integer.valueOf(configuration.get("product-key-segments"));
		productKeySegmentLength = Integer.valueOf(configuration.get("product-key-segment-length"));

		activationKeySegments = Integer.valueOf(configuration.get("activation-key-segments"));
		activationKeySegmentLength = Integer.valueOf(configuration.get("activation-key-segment-length"));

		cancelKeySegments = Integer.valueOf(configuration.get("cancel-key-segments"));
		cancelKeySegmentLength = Integer.valueOf(configuration.get("cancel-key-segment-length"));

		if (configuration.get("auth-frontend").equals("html-internal")) {
			Memory.getInstance().getOrCreate("usermanager.auth.frontend").assign(new HTMLFrontendLogin());
		} else if (configuration.get("auth-frontend").equals("modular")) {
			if (Memory.getInstance().get("usermanager.auth.frontend") == null) {
				Memory.getInstance().getOrCreate("usermanager.auth.frontend").assign(new DefaultAuthFrontend());
			}
		}

		for (Class<IAuthenticationBackend> impl : findClasses(getMainImplementation(), IAuthenticationBackend.class)) {
			if (!impl.isInterface()) {
				try {
					IAuthenticationBackend inst = impl.getConstructor().newInstance();
					if (inst.name().equals(configuration.get("authentication-backend"))) {
						if (inst.available()) {
							backend = inst;
						} else {
							backend = null;
							error("Authentication backend '" + configuration.get("authentication-backend")
									+ "' is unavailable in your current environment.");
							error("Please make sure all its dependencies are installed.");
							error("");
							error("UserManager module unavailable, 503 error will be thrown if the module is interfaced with,");
							error("Please fix the error and reload the server.");
						}
						return;
					}
				} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
						| InvocationTargetException | NoSuchMethodException | SecurityException e) {
					throw new RuntimeException(e);
				}
			}
		}

		backend = null;
		error("Invalid authentication backend: " + configuration.get("authentication-backend"));
		error("UserManager module unavailable, 503 error will be thrown if the module is interfaced with,");
		error("Please change the 'authentication-backend' configuration property and reload the server.");
	}

	public static String getCreateUserCommand() {
		return configuration.get("create-user");
	}

	public static String getMailTemplate() {
		return configuration.get("mail-template-activation");
	}

	public static String[] getMailCommand() {
		ArrayList<String> cmd = new ArrayList<String>();
		cmd.add(configuration.get("mailcommand"));
		cmd.addAll(parseCommand(configuration.get("mailarguments")));
		return cmd.toArray(t -> new String[t]);
	}

	public static String generate(String template, HashMap<String, String> vars) {
		String output = template;
		for (String key : vars.keySet()) {
			output = output.replaceAll("\\%" + key + "\\%", vars.get(key));
		}
		return output;
	}

	public static String[] generate(String[] template, HashMap<String, String> vars) {
		String[] output = template.clone();
		for (int i = 0; i < output.length; i++) {
			for (String key : vars.keySet()) {
				output[i] = output[i].replaceAll("\\%" + key + "\\%", vars.get(key));
			}
		}
		return output;
	}

	public static String getMailSender() {
		return configuration.get("mail-sender");
	}

	public static String getAuthCommand() {
		return configuration.get("authenticate");
	}

	public static String getAuthSecureStorageDir() {
		return configuration.get("auth-secure-storage");
	}

	public static File getActivatedUsersDir() {
		return userDataDir;
	}

	public static AuthSecureStorage getSecureStore(String group, String username, byte[] key) throws IOException {
		if (userStorage.containsKey(group + "." + username))
			return userStorage.get(group + "." + username);

		AuthSecureStorage storage = AuthSecureStorage.open(getStoreFile(group, username), key);
		userStorage.put(group + "." + username, storage);

		return storage;
	}

	public static File getStoreFile(String group, String username) {
		return new File(UserManagerModule.getAuthSecureStorageDir(), group + "." + username + ".sdc");
	}

	public static String getActivateCommand() {
		return configuration.get("activate-user");
	}

	public static String getCancelCommand() {
		return configuration.get("cancel-activation");
	}

	public static String getUpdateCommand() {
		return configuration.get("update-user");
	}

	public static void updateUserName(String group, String username, String newUserName) {
		AuthSecureStorage storage = userStorage.get(group + "." + username);
		if (storage != null) {
			userStorage.put(group + "." + newUserName, storage);
		}
		userStorage.remove(group + "." + username);
	}

	public static String getAdminCommand() {
		return configuration.get("admin-commands");
	}

	public static String getAdminGroup() {
		return configuration.get("admin-group");
	}

	public static String getDeleteCommand() {
		return configuration.get("remove-user");
	}

}
