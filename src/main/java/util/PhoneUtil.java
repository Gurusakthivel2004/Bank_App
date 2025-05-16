package util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

import enums.Constants.Country;

public class PhoneUtil {

	private static final Logger LOGGER = LogManager.getLogger(PhoneUtil.class);
	private static final PhoneNumberUtil PHONE_NUMBER_UTIL = PhoneNumberUtil.getInstance();

	public static String formatByCountryName(String phoneNumber, String countryName) {
		Country country = Country.fromCountryName(countryName);
		return formatToE164Internal(phoneNumber, country);
	}

	public static String formatByRegionCode(String phoneNumber, String regionCode) {
		Country country = Country.fromRegionCode(regionCode);
		return formatToE164Internal(phoneNumber, country);
	}

	private static String formatToE164Internal(String phoneNumber, Country country) {
		if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
			LOGGER.error("Phone number is null or empty.");
			return null;
		}

		if (country == null) {
			LOGGER.error("Unsupported country.");
			return null;
		}

		try {
			PhoneNumber numberProto = PHONE_NUMBER_UTIL.parse(phoneNumber, country.getRegionCode());
			if (PHONE_NUMBER_UTIL.isValidNumber(numberProto)) {
				String formattedNumber = PHONE_NUMBER_UTIL.format(numberProto, PhoneNumberUtil.PhoneNumberFormat.E164);
				LOGGER.info("Successfully formatted phone number to E.164: {}", formattedNumber);
				return formattedNumber;
			} else {
				LOGGER.warn("Phone number is invalid for country '{}': {}", country.getCountryName(), phoneNumber);
				return null;
			}
		} catch (NumberParseException e) {
			LOGGER.error("Failed to parse phone number '{}': {}", phoneNumber, e.getMessage());
			return null;
		}
	}

	public static boolean isValidPhoneNumber(String phoneNumber, String countryName) {
		if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
			LOGGER.error("Phone number is null or empty.");
			return false;
		}

		Country country = Country.fromCountryName(countryName);
		if (country == null) {
			LOGGER.error("Unsupported country: {}", countryName);
			return false;
		}

		try {
			PhoneNumber numberProto = PHONE_NUMBER_UTIL.parse(phoneNumber, country.getRegionCode());
			boolean isValid = PHONE_NUMBER_UTIL.isValidNumber(numberProto);
			LOGGER.info("Phone number '{}' validity for country '{}': {}", phoneNumber, countryName, isValid);
			return isValid;
		} catch (NumberParseException e) {
			LOGGER.error("Failed to parse phone number '{}': {}", phoneNumber, e.getMessage());
			return false;
		}
	}

}
