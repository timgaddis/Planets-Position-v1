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
#include "swiss/swephexp.h"

/*
 * Calculate the next solar eclipse after a given date.
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
jdoubleArray Java_planets_position_SolarEclipse_solarData(JNIEnv* env,
		jobject this, jdouble d_ut, jdoubleArray loc, jdouble atpress,
		jdouble attemp) {

	char serr[256];
	double g[3], attr[20], tret[10], az[6];
	int retval, i;

	jdoubleArray result;
	result = (*env)->NewDoubleArray(env, 8);
	if (result == NULL) {
		return NULL; /* out of memory error thrown */
	}

	(*env)->GetDoubleArrayRegion(env, loc, 0, 3, g);

	swe_set_ephe_path("/mnt/sdcard/ephemeris/");
	swe_set_topo(g[0], g[1], g[2]);

	retval = swe_sol_eclipse_when_glob(d_ut, SEFLG_SWIEPH, 0, tret, 0, serr);
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
		iflgret = swe_calc_ut(tret[0], 1, iflag, x2, serr);
		swe_azalt(tret[0], SE_EQU2HOR, g, 0, 0, x2, az);
		az[0] += 180;
		if (az[0] > 360)
			az[0] -= 360;

		swe_close();

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
