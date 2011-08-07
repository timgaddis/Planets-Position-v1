/*
 * Copyright (C) 2011 Tim Gaddis
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <android/log.h>
#include "swiss/swephexp.h"

/*
 * Calculate the geographic position of where a solar eclipse occurs for a
 * 		given date.
 * Swiss Ephemeris functions called:
 * 		swe_set_ephe_path
 * 		swe_sol_eclipse_where
 * 		swe_close
 * Input: Julian date in ut1.
 * Output: Double array containing longitude and latitude.
 */
jdoubleArray Java_planets_position_SolarEclipse_solarDataPos(JNIEnv* env,
		jobject this, jdouble d_ut) {

	char serr[256];
	double attr[20], g[2];
	int retval;

	jdoubleArray result;
	result = (*env)->NewDoubleArray(env, 2);
	if (result == NULL) {
		return NULL; /* out of memory error thrown */
	}

	swe_set_ephe_path("/mnt/sdcard/ephemeris/");

	swe_sol_eclipse_where(d_ut, SEFLG_SWIEPH, g, attr, serr);
	swe_close();

	// move from the temp structure to the java structure
	(*env)->SetDoubleArrayRegion(env, result, 0, 2, g);
	return result;
}

/*
 * Calculate the next solar eclipse globally after a given date.
 * Swiss Ephemeris functions called:
 * 		swe_set_ephe_path
 * 		swe_sol_eclipse_when_glob
 * 		swe_sol_eclipse_how
 * 		swe_close
 * Input: Julian date in ut1, search direction(0=forward|1=back).
 * Output: Double array containing eclipse type and eclipse event times.
 */
jdoubleArray Java_planets_position_SolarEclipse_solarDataGlobal(JNIEnv* env,
		jobject this, jdouble d_ut, jdoubleArray loc, jint back) {

	char serr[256];
	double tret[10], g[3], attr[20], rval, ii;
	int retval, i;

	jdoubleArray result;
	result = (*env)->NewDoubleArray(env, 10);
	if (result == NULL) {
		return NULL; /* out of memory error thrown */
	}

	(*env)->GetDoubleArrayRegion(env, loc, 0, 3, g);
	swe_set_ephe_path("/mnt/sdcard/ephemeris/");

	retval = swe_sol_eclipse_when_glob(d_ut, SEFLG_SWIEPH, 0, tret, back, serr);
	if (retval == ERR) {
		swe_close();
		return NULL;
	}

	i = swe_sol_eclipse_how(tret[0], SEFLG_SWIEPH, g, attr, serr);
	if (i == ERR) {
		swe_close();
		return NULL;
	}

	rval = retval * 1.0;
	ii = i * 1.0;
	swe_close();

	// move from the temp structure to the java structure
	(*env)->SetDoubleArrayRegion(env, result, 0, 1, &rval);
	(*env)->SetDoubleArrayRegion(env, result, 1, 8, tret);
	(*env)->SetDoubleArrayRegion(env, result, 9, 1, &ii);
	return result;
}

/*
 * Calculate the next solar eclipse locally after a given date.
 * Swiss Ephemeris functions called:
 * 		swe_set_ephe_path
 * 		swe_set_topo
 * 		swe_sol_eclipse_when_loc
 * 		swe_sol_eclipse_how
 * 		swe_calc_ut
 * 		swe_azalt
 * 		swe_close
 * Input: Julian date in ut1, location array, search direction(0=forward|1=back).
 * Output: Double array containing local eclipse type ,local eclipse event times,
 * 			eclipse attributes, and moon position
 */
jdoubleArray Java_planets_position_SolarEclipse_solarDataLocal(JNIEnv* env,
		jobject this, jdouble d_ut, jdoubleArray loc, jint back) {

	char serr[256];
	double g[3], attr[20], tret[10], az[6], x2[6], rval;
	int retval, i;
	int iflag = SEFLG_SWIEPH | SEFLG_EQUATORIAL | SEFLG_TOPOCTR;

	/*__android_log_print(ANDROID_LOG_INFO, "solarDataLocal", "date: %f", d_ut);*/

	jdoubleArray result;
	result = (*env)->NewDoubleArray(env, 19);
	if (result == NULL) {
		return NULL; /* out of memory error thrown */
	}

	(*env)->GetDoubleArrayRegion(env, loc, 0, 3, g);
	swe_set_ephe_path("/mnt/sdcard/ephemeris/");
	swe_set_topo(g[0], g[1], g[2]);

	retval = swe_sol_eclipse_when_loc(d_ut, SEFLG_SWIEPH, g, tret, attr, back,
			serr);
	if (retval == ERR) {
		swe_close();
		return NULL;
	} else {
		i = swe_sol_eclipse_how(tret[0], SEFLG_SWIEPH, g, attr, serr);
		if (i == ERR) {
			swe_close();
			return NULL;
		}

		// calculate moon position at max eclipse
		i = swe_calc_ut(tret[0], 1, iflag, x2, serr);
		if (i == ERR) {
			swe_close();
			return NULL;
		}
		swe_azalt(tret[0], SE_EQU2HOR, g, 0, 0, x2, az);
		az[0] += 180;
		if (az[0] > 360)
			az[0] -= 360;

		rval = retval * 1.0;
		swe_close();

		// move from the temp structure to the java structure
		(*env)->SetDoubleArrayRegion(env, result, 0, 1, &rval);
		(*env)->SetDoubleArrayRegion(env, result, 1, 5, tret);
		(*env)->SetDoubleArrayRegion(env, result, 6, 11, attr);
		(*env)->SetDoubleArrayRegion(env, result, 17, 2, az);
		return result;
	}
}

/*
 * Convert a calendar date ( year, month, day, hour, min, sec) to a Julian date.
 * Swiss Ephemeris function called:
 * 		swe_utc_to_jd
 * Input: year, month, day, hour, min, sec
 * Output: double array with Julian date in ut1 and tt values.
 */
jdoubleArray Java_planets_position_SolarEclipse_utc2jd(JNIEnv* env,
		jobject this, jint m, jint d, jint y, jint hr, jint min, jdouble sec) {

	char serr[256];
	double dret[2];
	int retval;
	jdoubleArray result;
	result = (*env)->NewDoubleArray(env, 5);
	if (result == NULL) {
		return NULL; /* out of memory error thrown */
	}

	retval = swe_utc_to_jd(y, m, d, hr, min, sec, SE_GREG_CAL, dret, serr);
	if (retval == ERR) {
		fprintf(stderr, serr); /* error handling */
		return NULL;
	}

	(*env)->SetDoubleArrayRegion(env, result, 0, 2, dret);
	return result;

}

/*
 * Covert a given Julian date to a calendar date in utc.
 * Swiss Ephemeris function called:
 * 		swe_jdut1_to_utc
 * Input: Julian date
 * Output: String containing a calendar date
 */
jstring Java_planets_position_SolarEclipse_jd2utc(JNIEnv* env, jobject this,
		jdouble juldate) {

	char *outFormat = "_%i_%i_%i_%i_%i_%2.1f_";
	char output[30];
	int i, y, mo, d, h, mi;
	double s;

	swe_jdut1_to_utc(juldate, SE_GREG_CAL, &y, &mo, &d, &h, &mi, &s);

	i = sprintf(output, outFormat, y, mo, d, h, mi, s);
	return (*env)->NewStringUTF(env, output);
}

/*
 * Calculate the position of a given planet in the sky.
 * Swiss Ephemeris functions called:
 * 		swe_set_ephe_path
 * 		swe_set_topo
 * 		swe_calc
 * 		swe_azalt
 * 		swe_pheno_ut
 * 		swe_rise_trans
 * 		swe_close
 * Input: Julian date in ephemeris time, Julian date in ut1, planet number,
 * 		location array, atmospheric pressure and temperature.
 * Output: Double array containing RA, Dec, distance, azimuth, altitude,
 * 		magnitude, set time, and rise time of planet.
 */
jdoubleArray Java_planets_position_LivePosition_planetLiveData(JNIEnv* env,
		jobject this, jdouble d_et, jdouble d_ut, jint p, jdoubleArray loc,
		jdouble atpress, jdouble attemp) {

	char serr[256];
	double x2[3], az[3], g[3], attr[20], setT, riseT;
	int iflag = SEFLG_SWIEPH | SEFLG_EQUATORIAL | SEFLG_TOPOCTR;
	int iflgret;

	jdoubleArray result;
	result = (*env)->NewDoubleArray(env, 8);
	if (result == NULL) {
		return NULL; /* out of memory error thrown */
	}

	(*env)->GetDoubleArrayRegion(env, loc, 0, 3, g);

	swe_set_ephe_path("/mnt/sdcard/ephemeris/");
	swe_set_topo(g[0], g[1], g[2]);
	iflgret = swe_calc(d_et, p, iflag, x2, serr);
	if (iflgret == ERR) {
		swe_close();
		return NULL;
	} else {
		swe_azalt(d_ut, SE_EQU2HOR, g, atpress, attemp, x2, az);
		swe_pheno_ut(d_ut, p, SEFLG_SWIEPH, attr, serr);
		swe_rise_trans(d_ut, p, "", SEFLG_SWIEPH, SE_CALC_SET, g, atpress,
				attemp, &setT, serr);
		swe_rise_trans(d_ut, p, "", SEFLG_SWIEPH, SE_CALC_RISE, g, atpress,
				attemp, &riseT, serr);
		swe_close();

		/*rotates azimuth origin to north*/
		az[0] += 180;
		if (az[0] > 360)
			az[0] -= 360;

		// move from the temp structure to the java structure
		(*env)->SetDoubleArrayRegion(env, result, 0, 3, x2);
		(*env)->SetDoubleArrayRegion(env, result, 3, 2, az);
		(*env)->SetDoubleArrayRegion(env, result, 5, 1, &attr[4]);
		(*env)->SetDoubleArrayRegion(env, result, 6, 1, &setT);
		(*env)->SetDoubleArrayRegion(env, result, 7, 1, &riseT);
		return result;
	}
}

/*
 * Convert a calendar date ( year, month, day, hour, min, sec) to a Julian date.
 * Swiss Ephemeris function called:
 * 		swe_utc_to_jd
 * Input: year, month, day, hour, min, sec
 * Output: double array with Julian date in ut1 and tt values.
 */
jdoubleArray Java_planets_position_LivePosition_utc2jd(JNIEnv* env,
		jobject this, jint m, jint d, jint y, jint hr, jint min, jdouble sec) {

	char serr[256];
	double dret[2];
	int retval;
	jdoubleArray result;
	result = (*env)->NewDoubleArray(env, 5);
	if (result == NULL) {
		return NULL; /* out of memory error thrown */
	}

	retval = swe_utc_to_jd(y, m, d, hr, min, sec, SE_GREG_CAL, dret, serr);
	if (retval == ERR) {
		fprintf(stderr, serr); /* error handling */
		return NULL;
	}

	(*env)->SetDoubleArrayRegion(env, result, 0, 2, dret);
	return result;

}

/*
 * Covert a given Julian date to a calendar date in utc.
 * Swiss Ephemeris function called:
 * 		swe_jdut1_to_utc
 * Input: Julian date
 * Output: String containing a calendar date
 */
jstring Java_planets_position_LivePosition_jd2utc(JNIEnv* env, jobject this,
		jdouble juldate) {

	char *outFormat = "_%i_%i_%i_%i_%i_%2.1f_";
	char output[30];
	int i, y, mo, d, h, mi;
	double s;

	swe_jdut1_to_utc(juldate, SE_GREG_CAL, &y, &mo, &d, &h, &mi, &s);

	i = sprintf(output, outFormat, y, mo, d, h, mi, s);
	return (*env)->NewStringUTF(env, output);
}

/*
 * Calculate the position of a given planet in the sky.
 * Swiss Ephemeris functions called:
 * 		swe_set_ephe_path
 * 		swe_set_topo
 * 		swe_calc
 * 		swe_azalt
 * 		swe_pheno_ut
 * 		swe_rise_trans
 * 		swe_close
 * Input: Julian date in ephemeris time, Julian date in ut1, planet number,
 * 		location array, atmospheric pressure and temperature.
 * Output: Double array containing RA, Dec, distance, azimuth, altitude,
 * 		magnitude, set time, and rise time of planet.
 */
jdoubleArray Java_planets_position_Position_planetPosData(JNIEnv* env,
		jobject this, jdouble d_et, jdouble d_ut, jint p, jdoubleArray loc,
		jdouble atpress, jdouble attemp) {

	char serr[256];
	double x2[3], az[3], g[3], attr[20], setT, riseT;
	int iflag = SEFLG_SWIEPH | SEFLG_EQUATORIAL | SEFLG_TOPOCTR;
	int iflgret;

	jdoubleArray result;
	result = (*env)->NewDoubleArray(env, 8);
	if (result == NULL) {
		return NULL; /* out of memory error thrown */
	}

	(*env)->GetDoubleArrayRegion(env, loc, 0, 3, g);

	swe_set_ephe_path("/mnt/sdcard/ephemeris/");
	swe_set_topo(g[0], g[1], g[2]);
	iflgret = swe_calc(d_et, p, iflag, x2, serr);
	if (iflgret == ERR) {
		swe_close();
		return NULL;
	} else {
		swe_azalt(d_ut, SE_EQU2HOR, g, atpress, attemp, x2, az);
		swe_pheno_ut(d_ut, p, SEFLG_SWIEPH, attr, serr);
		swe_rise_trans(d_ut, p, "", SEFLG_SWIEPH, SE_CALC_SET, g, atpress,
				attemp, &setT, serr);
		swe_rise_trans(d_ut, p, "", SEFLG_SWIEPH, SE_CALC_RISE, g, atpress,
				attemp, &riseT, serr);
		swe_close();

		/*rotates azimuth origin to north*/
		az[0] += 180;
		if (az[0] > 360)
			az[0] -= 360;

		// move from the temp structure to the java structure
		(*env)->SetDoubleArrayRegion(env, result, 0, 3, x2);
		(*env)->SetDoubleArrayRegion(env, result, 3, 2, az);
		(*env)->SetDoubleArrayRegion(env, result, 5, 1, &attr[4]);
		(*env)->SetDoubleArrayRegion(env, result, 6, 1, &setT);
		(*env)->SetDoubleArrayRegion(env, result, 7, 1, &riseT);
		return result;
	}
}

/*
 * Convert a calendar date ( year, month, day, hour, min, sec) to a Julian date.
 * Swiss Ephemeris function called:
 * 		swe_utc_to_jd
 * Input: year, month, day, hour, min, sec
 * Output: double array with Julian date in ut1 and tt values.
 */
jdoubleArray Java_planets_position_Position_utc2jd(JNIEnv* env, jobject this,
		jint m, jint d, jint y, jint hr, jint min, jdouble sec) {

	char serr[256];
	double dret[2];
	int retval;
	jdoubleArray result;
	result = (*env)->NewDoubleArray(env, 5);
	if (result == NULL) {
		return NULL; /* out of memory error thrown */
	}

	retval = swe_utc_to_jd(y, m, d, hr, min, sec, SE_GREG_CAL, dret, serr);
	if (retval == ERR) {
		fprintf(stderr, serr); /* error handling */
		return NULL;
	}

	(*env)->SetDoubleArrayRegion(env, result, 0, 2, dret);
	return result;

}

/*
 * Covert a given Julian date to a calendar date in utc.
 * Swiss Ephemeris function called:
 * 		swe_jdut1_to_utc
 * Input: Julian date
 * Output: String containing a calendar date
 */
jstring Java_planets_position_Position_jd2utc(JNIEnv* env, jobject this,
		jdouble juldate) {

	char *outFormat = "_%i_%i_%i_%i_%i_%2.1f_";
	char output[30];
	int i, y, mo, d, h, mi;
	double s;

	swe_jdut1_to_utc(juldate, SE_GREG_CAL, &y, &mo, &d, &h, &mi, &s);

	i = sprintf(output, outFormat, y, mo, d, h, mi, s);
	return (*env)->NewStringUTF(env, output);
}

/*
 * Calculate the position of a given planet in the sky.
 * Swiss Ephemeris functions called:
 * 		swe_set_ephe_path
 * 		swe_set_topo
 * 		swe_calc
 * 		swe_azalt
 * 		swe_pheno_ut
 * 		swe_rise_trans
 * 		swe_close
 * Input: Julian date in ephemeris time, Julian date in ut1, planet number,
 * 		location array, atmospheric pressure and temperature.
 * Output: Double array containing RA, Dec, distance, azimuth, altitude,
 * 		magnitude, and set time of planet.
 */
jdoubleArray Java_planets_position_ViewWhatsUp_planetUpData(JNIEnv* env,
		jobject this, jdouble d_et, jdouble d_ut, jint p, jdoubleArray loc,
		jdouble atpress, jdouble attemp) {

	char serr[256];
	double x2[3], az[3], g[3], attr[20], setT;
	int iflag = SEFLG_SWIEPH | SEFLG_EQUATORIAL | SEFLG_TOPOCTR;
	int iflgret;

	jdoubleArray result;
	result = (*env)->NewDoubleArray(env, 7);
	if (result == NULL) {
		return NULL; /* out of memory error thrown */
	}

	(*env)->GetDoubleArrayRegion(env, loc, 0, 3, g);

	swe_set_ephe_path("/mnt/sdcard/ephemeris/");
	swe_set_topo(g[0], g[1], g[2]);
	iflgret = swe_calc(d_et, p, iflag, x2, serr);
	if (iflgret == ERR) {
		swe_close();
		return NULL;
	} else {
		swe_azalt(d_ut, SE_EQU2HOR, g, atpress, attemp, x2, az);
		swe_pheno_ut(d_ut, p, SEFLG_SWIEPH, attr, serr);
		swe_rise_trans(d_ut, p, "", SEFLG_SWIEPH, SE_CALC_SET, g, atpress,
				attemp, &setT, serr);
		swe_close();

		/*rotates azimuth origin to north*/
		az[0] += 180;
		if (az[0] > 360)
			az[0] -= 360;

		// move from the temp structure to the java structure
		(*env)->SetDoubleArrayRegion(env, result, 0, 3, x2);
		(*env)->SetDoubleArrayRegion(env, result, 3, 2, az);
		(*env)->SetDoubleArrayRegion(env, result, 5, 1, &attr[4]);
		(*env)->SetDoubleArrayRegion(env, result, 6, 1, &setT);
		return result;
	}
}

/*
 * Convert a calendar date ( year, month, day, hour, min, sec) to a Julian date.
 * Swiss Ephemeris function called:
 * 		swe_utc_to_jd
 * Input: year, month, day, hour, min, sec
 * Output: double array with Julian date in ut1 and tt values.
 */
jdoubleArray Java_planets_position_ViewWhatsUp_utc2jd(JNIEnv* env, jobject this,
		jint m, jint d, jint y, jint hr, jint min, jdouble sec) {

	char serr[256];
	double dret[2];
	int retval;
	jdoubleArray result;
	result = (*env)->NewDoubleArray(env, 5);
	if (result == NULL) {
		return NULL; /* out of memory error thrown */
	}

	retval = swe_utc_to_jd(y, m, d, hr, min, sec, SE_GREG_CAL, dret, serr);
	if (retval == ERR) {
		fprintf(stderr, serr); /* error handling */
		return NULL;
	}

	(*env)->SetDoubleArrayRegion(env, result, 0, 2, dret);
	return result;

}

/*
 * Covert a given Julian date to a calendar date in utc.
 * Swiss Ephemeris function called:
 * 		swe_jdut1_to_utc
 * Input: Julian date
 * Output: String containing a calendar date
 */
jstring Java_planets_position_ViewWhatsUp_jd2utc(JNIEnv* env, jobject this,
		jdouble juldate) {

	char *outFormat = "_%i_%i_%i_%i_%i_%2.1f_";
	char output[30];
	int i, y, mo, d, h, mi;
	double s;

	swe_jdut1_to_utc(juldate, SE_GREG_CAL, &y, &mo, &d, &h, &mi, &s);

	i = sprintf(output, outFormat, y, mo, d, h, mi, s);
	return (*env)->NewStringUTF(env, output);
}
