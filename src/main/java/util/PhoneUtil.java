package util;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PhoneUtil {

	private static final Logger LOGGER = LogManager.getLogger(PhoneUtil.class);
	private static final PhoneNumberUtil PHONE_NUMBER_UTIL = PhoneNumberUtil.getInstance();

	public static String formatToE164(String phoneNumber, String regionCode) {
		if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
			LOGGER.error("Phone number is null or empty.");
			return null;
		}

		try {
			PhoneNumber numberProto = PHONE_NUMBER_UTIL.parse(phoneNumber, regionCode);
			if (PHONE_NUMBER_UTIL.isValidNumber(numberProto)) {
				String formattedNumber = PHONE_NUMBER_UTIL.format(numberProto, PhoneNumberUtil.PhoneNumberFormat.E164);
				LOGGER.info("Successfully formatted phone number to E.164: {}", formattedNumber);
				return formattedNumber;
			} else {
				LOGGER.warn("Phone number is invalid according to region {}: {}", regionCode, phoneNumber);
				return null;
			}
		} catch (NumberParseException e) {
			LOGGER.error("Failed to parse phone number '{}': {}", phoneNumber, e.getMessage());
			return null;
		}
	}

	public static boolean isValidPhoneNumber(String phoneNumber, String regionCode) {
		if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
			LOGGER.error("Phone number is null or empty.");
			return false;
		}

		try {
			PhoneNumber numberProto = PHONE_NUMBER_UTIL.parse(phoneNumber, regionCode);
			boolean isValid = PHONE_NUMBER_UTIL.isValidNumber(numberProto);
			LOGGER.info("Phone number '{}' validity for region '{}': {}", phoneNumber, regionCode, isValid);
			return isValid;
		} catch (NumberParseException e) {
			LOGGER.error("Failed to parse phone number '{}': {}", phoneNumber, e.getMessage());
			return false;
		}
	}
}
