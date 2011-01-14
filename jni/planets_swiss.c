/*
 * Copyright (C) 2010 Tim Gaddis
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

jdoubleArray Java_planets_position_Position_planetRADec(JNIEnv* env,
		jobject this, jdouble d_et, jdouble d_ut, jint p, jdoubleArray loc,
		jdouble atpress, jdouble attemp) {

	char serr[256];
	double x2[3], az[3], g[3];
	int iflag = SEFLG_SWIEPH | SEFLG_EQUATORIAL | SEFLG_TOPOCTR;
	int iflgret, i;

	jdoubleArray result;
	result = (*env)->NewDoubleArray(env, 5);
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
		swe_close();

		/*rotates azimuth origin to north*/
		az[0] += 180;
		if (az[0] > 360)
			az[0] -= 360;

		// move from the temp structure to the java structure
		(*env)->SetDoubleArrayRegion(env, result, 0, 3, x2);
		(*env)->SetDoubleArrayRegion(env, result, 3, 2, az);
		return result;
	}
}

jdoubleArray Java_planets_position_Position_utc2jd(JNIEnv* env, jobject this,
		jint m, jint d, jint y, jint hr, jint min, jdouble sec) {

	char serr[256];
	double dret[2];
	int retval, i;
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

jdoubleArray Java_planets_position_ViewWhatsUp_planetRADec(JNIEnv* env,
		jobject this, jdouble d_et, jdouble d_ut, jint p, jdoubleArray loc,
		jdouble atpress, jdouble attemp) {

	char serr[256];
	double x2[3], az[3], g[3];
	int iflag = SEFLG_SWIEPH | SEFLG_EQUATORIAL | SEFLG_TOPOCTR;
	int iflgret, i;

	jdoubleArray result;
	result = (*env)->NewDoubleArray(env, 5);
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
		swe_close();

		/*rotates azimuth origin to north*/
		az[0] += 180;
		if (az[0] > 360)
			az[0] -= 360;

		// move from the temp structure to the java structure
		(*env)->SetDoubleArrayRegion(env, result, 0, 3, x2);
		(*env)->SetDoubleArrayRegion(env, result, 3, 2, az);
		return result;
	}
}

jdoubleArray Java_planets_position_ViewWhatsUp_utc2jd(JNIEnv* env,
		jobject this, jint m, jint d, jint y, jint hr, jint min, jdouble sec) {

	char serr[256];
	double dret[2];
	int retval, i;
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
