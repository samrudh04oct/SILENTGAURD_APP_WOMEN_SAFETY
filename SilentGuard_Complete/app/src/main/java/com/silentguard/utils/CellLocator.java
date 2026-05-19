package com.silentguard.utils;

import android.content.Context;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.TelephonyManager;
import android.util.Log;
import java.util.List;

public class CellLocator {

    private static final String TAG = "CellLocator";

    // ── FULL CELL DATA ────────────────────────────────────────────────────
    public static class CellData {
        public String type;       // LTE, GSM, WCDMA
        public int    mcc;        // Mobile Country Code  e.g. 404 = India
        public int    mnc;        // Mobile Network Code  e.g. 20 = Vodafone IN
        public int    lac;        // Location Area Code   (GSM/WCDMA)
        public int    tac;        // Tracking Area Code   (LTE)
        public int    cid;        // Cell ID
        public int    signal;     // Signal strength dBm
        public boolean registered;

        // Human readable names
        public String countryName;
        public String operatorName;
        public String areaCodeLabel; // "LAC" or "TAC"
        public int    areaCode;      // value of LAC or TAC

        public String toSmsBlock() {
            return "--- Cell Tower Info ---\n"
                + "Type   : " + type + "\n"
                + "Country: " + countryName + " (MCC=" + mcc + ")\n"
                + "Network: " + operatorName + " (MNC=" + mnc + ")\n"
                + areaCodeLabel + "    : " + areaCode + "\n"
                + "Cell ID: " + cid + "\n"
                + "Signal : " + signal + " dBm\n"
                + "-----------------------";
        }
    }

    public static class CellLocation {
        public double lat;
        public double lng;
        public String mapsLink;
        public boolean isEstimated;

        public CellLocation(double lat, double lng) {
            this.lat         = lat;
            this.lng         = lng;
            this.mapsLink    = "https://maps.google.com/?q=" + lat + "," + lng;
            this.isEstimated = true;
        }
    }

    // ── GET PRIMARY CELL DATA ─────────────────────────────────────────────
    @SuppressWarnings("MissingPermission")
    public static CellData getPrimaryCellData(Context context) {
        try {
            TelephonyManager tm = (TelephonyManager)
                context.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm == null) return null;

            List<CellInfo> cells = tm.getAllCellInfo();
            if (cells == null || cells.isEmpty()) return null;

            for (CellInfo cell : cells) {
                if (!cell.isRegistered()) continue;
                CellData data = new CellData();
                data.registered = true;

                if (cell instanceof CellInfoLte) {
                    CellIdentityLte     id = ((CellInfoLte) cell).getCellIdentity();
                    CellSignalStrengthLte sg = ((CellInfoLte) cell).getCellSignalStrength();
                    data.type   = "LTE (4G)";
                    data.mcc    = id.getMcc();
                    data.mnc    = id.getMnc();
                    data.tac    = id.getTac();
                    data.lac    = 0;
                    data.cid    = id.getCi();
                    data.signal = sg.getDbm();
                    data.areaCodeLabel = "TAC";
                    data.areaCode      = data.tac;

                } else if (cell instanceof CellInfoGsm) {
                    CellIdentityGsm      id = ((CellInfoGsm) cell).getCellIdentity();
                    CellSignalStrengthGsm sg = ((CellInfoGsm) cell).getCellSignalStrength();
                    data.type   = "GSM (2G)";
                    data.mcc    = id.getMcc();
                    data.mnc    = id.getMnc();
                    data.lac    = id.getLac();
                    data.tac    = 0;
                    data.cid    = id.getCid();
                    data.signal = sg.getDbm();
                    data.areaCodeLabel = "LAC";
                    data.areaCode      = data.lac;

                } else if (cell instanceof CellInfoWcdma) {
                    CellIdentityWcdma id = ((CellInfoWcdma) cell).getCellIdentity();
                    data.type   = "WCDMA (3G)";
                    data.mcc    = id.getMcc();
                    data.mnc    = id.getMnc();
                    data.lac    = id.getLac();
                    data.tac    = 0;
                    data.cid    = id.getCid();
                    data.signal = -90;
                    data.areaCodeLabel = "LAC";
                    data.areaCode      = data.lac;

                } else { continue; }

                data.countryName  = getMccCountry(data.mcc);
                data.operatorName = getMncOperator(data.mcc, data.mnc);
                return data;
            }
        } catch (Exception e) {
            Log.e(TAG, "getCellData: " + e.getMessage());
        }
        return null;
    }

    // ── ESTIMATE COORDINATES FROM CELL DATA ───────────────────────────────
    public static CellLocation estimateLocation(Context context) {
        CellData cell = getPrimaryCellData(context);
        if (cell == null) return null;

        double[] base = getCountryBase(cell.mcc);
        if (base == null) return null;

        double baseLat = base[0], baseLng = base[1], spread = base[2];
        int    area    = cell.areaCode;
        double lacNorm = normalize(area,    0, 65535);
        double cidNorm = normalize(cell.cid, 0, "LTE (4G)".equals(cell.type)
            ? 268435455 : 65535);
        double cidW    = "LTE (4G)".equals(cell.type) ? 0.3 : 1.0;

        double estLat = baseLat + (lacNorm - 0.5) * spread;
        double estLng = baseLng + (cidNorm * cidW - 0.5) * spread;

        double[] bounds = getCountryBounds(cell.mcc);
        if (bounds != null) {
            estLat = clamp(estLat, bounds[0], bounds[1]);
            estLng = clamp(estLng, bounds[2], bounds[3]);
        }

        Log.d(TAG, "Cell estimate: " + estLat + "," + estLng);
        return new CellLocation(estLat, estLng);
    }

    // ── BUILD FULL SMS WITH CELL INFO ─────────────────────────────────────
    public static String buildCellSmsBlock(Context context) {
        CellData cell = getPrimaryCellData(context);
        if (cell == null) return "Cell tower data unavailable.";
        return cell.toSmsBlock();
    }

    // ── MCC → COUNTRY NAME ────────────────────────────────────────────────
    public static String getMccCountry(int mcc) {
        switch (mcc) {
            case 404: case 405: return "India";
            case 310: case 311: case 312: case 313:
            case 314: case 315: case 316: return "USA";
            case 234: case 235: return "United Kingdom";
            case 505:           return "Australia";
            case 413:           return "Sri Lanka";
            case 410: case 412: return "Pakistan";
            case 460: case 461: return "China";
            case 440: case 441: return "Japan";
            case 450:           return "South Korea";
            case 502:           return "Malaysia";
            case 515:           return "Philippines";
            case 520:           return "Thailand";
            case 525:           return "Singapore";
            case 228:           return "Switzerland";
            case 262:           return "Germany";
            case 208:           return "France";
            case 222:           return "Italy";
            case 214:           return "Spain";
            case 429:           return "Nepal";
            case 470:           return "Bangladesh";
            default:            return "Unknown (MCC=" + mcc + ")";
        }
    }

    // ── MCC+MNC → OPERATOR NAME ───────────────────────────────────────────
    public static String getMncOperator(int mcc, int mnc) {
        if (mcc == 404 || mcc == 405) {
            switch (mnc) {
                case 1: case 2: case 3: case 4: case 5: case 6:
                case 7: case 44: case 53: case 54: case 64:
                case 79: case 94: case 98:          return "BSNL";
                case 10: case 49: case 70: case 77:
                case 80: case 90: case 93: case 95:
                case 96:                            return "Airtel";
                case 11: case 55: case 68: case 69: return "MTNL";
                case 12: case 13: case 20: case 43:
                case 46: case 56: case 58: case 60:
                case 75: case 76: case 84: case 86:
                case 88:                            return "Vodafone";
                case 15: case 16: case 41: case 51:
                case 62: case 66: case 67: case 78:
                case 81: case 91: case 92: case 97: return "Aircel";
                case 17: case 45:                   return "MTNL Delhi";
                case 30:                            return "Videcon";
                case 52: case 83: case 85:          return "Reliance";
                case 71: case 72: case 73: case 74:
                case 82: case 87: case 89:          return "Idea";
                case 750: case 840: case 861:       return "Reliance Jio";
                default:                            return "Indian Operator (MNC=" + mnc + ")";
            }
        }
        if (mcc == 310 || mcc == 311) {
            switch (mnc) {
                case 410:  return "AT&T";
                case 260:  return "T-Mobile";
                case 120:  return "Sprint";
                case 4:    return "Verizon";
                default:   return "US Operator (MNC=" + mnc + ")";
            }
        }
        if (mcc == 234 || mcc == 235) {
            switch (mnc) {
                case 10: return "O2 UK";
                case 20: return "Three UK";
                case 30: return "EE";
                case 21: return "Vodafone UK";
                default: return "UK Operator (MNC=" + mnc + ")";
            }
        }
        return "Operator (MNC=" + mnc + ")";
    }

    // ── COUNTRY BASE COORDS ───────────────────────────────────────────────
    private static double[] getCountryBase(int mcc) {
        switch (mcc) {
            case 404: case 405: return new double[]{20.5937,  78.9629, 12.0};
            case 310: case 311: return new double[]{37.0902, -95.7129, 20.0};
            case 234: case 235: return new double[]{55.3781,  -3.4360,  5.0};
            case 505:           return new double[]{-25.2744, 133.7751, 18.0};
            case 413:           return new double[]{7.8731,   80.7718,  2.0};
            case 410: case 412: return new double[]{30.3753,  69.3451,  8.0};
            case 460: case 461: return new double[]{35.8617, 104.1954, 18.0};
            case 440: case 441: return new double[]{36.2048, 138.2529,  8.0};
            case 450:           return new double[]{35.9078, 127.7669,  3.0};
            case 502:           return new double[]{4.2105,  101.9758,  5.0};
            case 515:           return new double[]{12.8797, 121.7740,  8.0};
            case 520:           return new double[]{15.8700, 100.9925,  5.0};
            case 525:           return new double[]{1.3521,  103.8198,  0.5};
            case 228:           return new double[]{46.8182,   8.2275,  2.0};
            case 262:           return new double[]{51.1657,  10.4515,  4.0};
            case 208:           return new double[]{46.2276,   2.2137,  4.0};
            case 222:           return new double[]{41.8719,  12.5674,  4.0};
            case 214:           return new double[]{40.4637,  -3.7492,  4.0};
            default:
                double lat = (mcc % 180) - 90;
                double lng = ((mcc * 7) % 360) - 180;
                return new double[]{lat, lng, 5.0};
        }
    }

    private static double[] getCountryBounds(int mcc) {
        switch (mcc) {
            case 404: case 405: return new double[]{6.0,  37.0,  68.0,  97.0};
            case 310: case 311: return new double[]{24.0, 72.0, -125.0, -66.0};
            case 234: case 235: return new double[]{49.0, 61.0,  -8.0,   2.0};
            case 505:           return new double[]{-44.0,-10.0, 113.0, 154.0};
            default:            return null;
        }
    }

    private static double normalize(int val, int min, int max) {
        if (max <= min) return 0.5;
        return Math.max(0.0, Math.min(1.0, (double)(val - min) / (max - min)));
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
