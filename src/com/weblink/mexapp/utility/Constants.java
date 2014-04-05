package com.weblink.mexapp.utility;

public class Constants {
	public static final String SERVER_URL = "https://callapi.weblink.se";
	// public static final String SERVER_URL = "https://devcallapi.weblink.se";
	public static final String SOCKETIO_SERVER_URL = "http://v3.webcall.se:443/callpopup";
	// public static final String SOCKETIO_SERVER_URL = "http://v3.pbxdesign.pbx.weblink.se:443/callpopup";

	public static final String[] WEB_CALL_STATUSES = new String[] { "", "Lunch", "Egen text", "Möte", "Kundbesök", "Tillfälligt ute", "Borta för dagen", "Affärsresa", "Sjuk", "Barnomsorg",
			"Semester", "Föräldraledighet", "Ledig" };

	public static final String CALL_DIRECTION_OUT = "OUT";
	public static final String CALL_DIRECTION_IN = "IN";

	public static final String CALL_STATUS_RINGING = "R";
	public static final String CALL_STATUS_CONNECTED = "C";
	public static final String CALL_STATUS_HOLD = "H";

	public static final String DEFAULT_VOICEMAIL_NUMBER = "555";

	public static final int AVAILABILITY_OFFLINE = 0;
	public static final int AVAILABILITY_AVAILABLE = 1;
	public static final int AVAILABILITY_BUSY = 2;
	public static final int AVAILABILITY_IN_CALL = 3;

	public static final int MISC_ITEM_SETTINGS = 0;
	public static final int MISC_ITEM_USER = 1;
	public static final int MISC_ITEM_FOLLOWME = 2;
	public static final int MISC_ITEM_FORWARDING = 3;
	public static final int MISC_ITEM_CALLS = 4;
	public static final int MISC_ITEM_ABOUT = 5;
	public static final int MISC_ITEM_HELP = 6;
	public static final int MISC_ITEM_LOGOFF = 7;

	public static final int THEME_DARK = 2;
	public static final int THEME_BLUE = 1;
	public static final int THEME_LIGHT = 0;
	public static final int ALARM_ID_STATUS = 1;
	public static final int ALARM_ID_VOICEMAIL = 2;
	public static final int ALARM_ID_PRESENCE = 3;

	public static final int NOTIFICATION_ID_CALLS = 1293;
	public static final int NOTIFICATION_ID_VOICEMAIL = 1295;

	public static final int CONTACT_TYPE_EXTENSION = 0;
	public static final int CONTACT_TYPE_LOCAL = 1;
	public static final int CONTACT_TYPE_WEBCALL = 2;

	public static final int COLOR_TINT_AVAILABLE = 0x5500BB00;
	public static final int COLOR_TINT_BUSY = 0x55CC0000;
	public static final int COLOR_TINT_INCALL = 0x55FFA500;
	public static final int COLOR_TINT_TRANSPARENT = 0x00000000;

	public static final int TRANSFER_START = 0;
	public static final int TRANSFER_FINISH = 1;
	public static final int TRANSFER_CANCEL = 2;
	public static final String TRANSFER_CHANNEL = "transfer_channel";

	public static final String CALL_HELD = "call_held";
	public static final int CALL_WINDOW_NONE = 0;
	public static final int CALL_WINDOW_TOP = 1;
	public static final int CALL_WINDOW_CENTER = 2;
	public static final int CALL_WINDOW_BOTTOM = 3;

	public static final String LOGIN_HASH = "login_hash";

	public static final String SHOW_LOCAL_CONTACTS = "show_local_contacts";
	public static final String SHOW_CONTACT_TYPE = "show_contact_type";

	public static final String CONNECTION_KEEP_ALIVE = "connection_keep_alive";
	public static final String CURRENT_STATUS = "current_status";
	public static final String CURRENT_STATUS_TEXT = "current_status_text";
	public static final String CURRENT_OUTGOING_NUMBER = "current_outgoing_number";

	public static final String DEFAULT_PRESENCE = "default_presence";
	public static final String DEFAULT_PRESENCE_ID = "default_presence_id";
	public static final String SCHEDULE_PRESENCE_MILLIS = "presence_schedule_millis";
	public static final String SCHEDULE_MEX_MILLIS = "schedule_presence_millis";

	public static final String FORWARDING_NUMBER_ALWAYS = "forwarding_number_always";
	public static final String FORWARDING_NUMBER_NOT_AVAIL = "forwarding_number_not_avail";
	public static final String FORWARDING_NUMBER_BUSY = "forwarding_number_busy";

	public static final String FOLLOWME_ENABLE = "followme_enable";
	public static final String FOLLOWME_CONFIRMATION = "followme_conf";
	public static final String FOLLOWME_MAIN_DURATION = "followme_main";
	public static final String FOLLOWME_LIST_DURATION = "followme_list";
	public static final String FOLLOWME_LIST = "followme_numbers";

	public static final String ALLOW_CALL_WAITING = "allow_call_waiting";
	public static final String DO_NOT_DISTURB = "do_not_disturb";

	public static final String ENABLE_CALL_FORWARDING_ALWAYS = "enable_call_forwarding";
	public static final String ENABLE_CALL_FORWARDING_BUSY = "enable_call_forwarding_busy";
	public static final String ENABLE_CALL_FORWARDING_NO_ANSWER = "enable_call_forwarding_na";

	public static final String MEX_LOGGED_IN = "mex_logged_in";
	public static final String MISC_ITEM_ICONS = "misc_item_icons";
	public static final String MISC_ITEM_STRINGS = "misc_item_strings";

	public static final String LOGIN_COMPANY = "login_company";
	public static final String LOGIN_EXTENSION = "login_extension";
	public static final String LOGIN_PASSWORD = "login_password";
	public static final String LOGIN_NAME = "user_name";
	public static final String SIGNED_IN = "signed_in";

	public static final String POSS_OUTGOING_NUMBERS = "possible_outgoing";
	public static final String POSS_WC_MESSAGES = "possible_wc_statuses";
	public static final String POSS_WC_MESSAGE_IDS = "possible_wc_ids";
	public static final String USES_DATATAL = "uses_datatal";

	public static final String SETTING_STARTUP_TAB = "startup_tab";
	public static final String SETTING_CALL_LOCAL = "call_local";
	public static final String SETTING_THEME = "theme_app";
	public static final String SETTING_UPDATE_FREQUENCY = "update_frequency";
	public static final String SETTING_SHORTCUT = "shortcut_selection";
	public static final String SETTING_CALL_WINDOW = "use_call_window";
	public static final String SETTING_VOICEMAIL_NUMBER = "voicemail_number";

	public static final String USER_AVAILABILITY = "user_availability";
	public static final String USER_FIRST_NAME = "user_first_name";
	public static final String USER_LAST_NAME = "user_last_name";
	public static final String USER_EMAIL = "user_email";
	public static final String USER_HAS_MEX = "user_has_mex";
	public static final String USER_HOME_PHONE = "user_ph";
	public static final String USER_MOBILE_PHONE = "user_pm";
	public static final String USER_PASSWORD = "user_pass";
	public static final String USER_RINGTIME = "user_ringtime";
	public static final String USER_FORW_RINGTIME = "user_cfringtime";

	public static final String LATEST_ACKED_VOICEMAIL = "latest_ignored_voicemail";
	public static final String LATEST_VOICEMAIL = "latest_voicemail";
	public static final String NEW_VOICEMAIL = "new_voicemail";
	public static final String NUMBER_OF_VOICEMAIL = "number_of_voicemail";
	public static final String VOICEMAIL_SHOW_DIALOG = "voicemail_show_dialog";

	public static final String WINDOW_WIDTH = "window_width";
	public static final String WINDOW_HEIGHT = "window_height";
	public static final String FIRST_ACTIVE_CALL_ID = "first_active_call_id";
	public static final String CALL_WINDOW_DISPLAYED = "call_window_displayed";
	public static final String SHOW_CALL_HISTORY = "show_call_history";

}
