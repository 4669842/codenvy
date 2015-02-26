/*
 * CODENVY CONFIDENTIAL
 * __________________
 * 
 *  [2012] - [2015] Codenvy, S.A. 
 *  All Rights Reserved.
 * 
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.api.dao.sql;

import com.codenvy.api.account.billing.PaymentState;

import java.util.concurrent.TimeUnit;

/**
 * Set of SQL queries
 *
 * @author Sergii Kabashniuk
 */
public interface SqlDaoQueries {
    /**
     * Multiplier to transform GB/h to MB/msec back and forth.
     */
    long MBMSEC_TO_GBH_MULTIPLIER = TimeUnit.HOURS.toMillis(1) * 1024;

    String GBH_SUM =
            " SUM(ROUND(M.FAMOUNT * ( upper(M.FDURING * ?)-lower(M.FDURING * ?)-1 )/" + MBMSEC_TO_GBH_MULTIPLIER + ".0 ,6)) ";


    String TOTAL_SUM = "ROUND(SUM(ROUND(FPAID_AMOUNT,2)*FPAID_PRICE),2)";

    String INVOICES_FIELDS =
            "                   FID, " +
            "                   FTOTAL, " +
            "                   FACCOUNT_ID, " +
            "                   FCREDIT_CARD, " +
            "                   FPAYMENT_TIME, " +
            "                   FPAYMENT_STATE, " +
            "                   FMAILING_TIME, " +
            "                   FCREATED_TIME, " +
            "                   FPERIOD, " +
            "                   FCALC_ID ";

    /**
     * SQL query to calculate memory charges metrics.
     * Metrics transformed from MB/msec  to  GB/h and rounded with precision 6 before aggregation.
     */
    String MEMORY_CHARGES_INSERT =
            "INSERT INTO " +
            "  MEMORY_CHARGES (" +
            "                   FAMOUNT, " +
            "                   FACCOUNT_ID, " +
            "                   FWORKSPACE_ID,  " +
            "                   FCALC_ID " +
            "                  ) " +
            "SELECT " +
            "  " + GBH_SUM + " AS FAMOUNT, " +
            "   M.FACCOUNT_ID, " +
            "   M.FWORKSPACE_ID,  " +
            "   ? AS FCALC_ID  " +
            "FROM " +
            "  METRICS AS M " +
            "WHERE " +
            "   M.FDURING && ?" +
            "GROUP BY " +
            " M.FACCOUNT_ID, " +
            " M.FWORKSPACE_ID ";

    String PREPAID_AMOUNT =
            " SUM(P.FAMOUNT*(upper(P.FPERIOD * ?)-lower(P.FPERIOD * ?))/?)";


    String ACCOUNT_USAGE_SELECT = "SELECT " +
                                  "   M.FACCOUNT_ID AS FACCOUNT_ID, " +
                                  "   ROUND(CAST(LEAST("+GBH_SUM+", ?) as numeric), 6) AS FFREE_AMOUNT, " +
                                  "   ROUND(CAST(LEAST(GREATEST("+GBH_SUM+" -?, 0), CASE WHEN P.FAMOUNT IS NULL THEN 0.0 ELSE P.FAMOUNT END) as numeric), 6) AS FPREPAID_AMOUNT, " +
                                  "   ROUND(CAST(GREATEST("+GBH_SUM+" - ? -  CASE WHEN P.FAMOUNT IS NULL THEN 0.0 ELSE P.FAMOUNT END , 0) as numeric), 6) AS FPAID_AMOUNT " +
                                  "FROM " +
                                  "   METRICS  AS M " +
                                  "  LEFT JOIN ( " +
                                  "      SELECT " +
                                  "        " + PREPAID_AMOUNT + " AS FAMOUNT, " +
                                  "        FACCOUNT_ID " +
                                  "      FROM  " +
                                  "        PREPAID AS P" +
                                  "      WHERE  " +
                                  "        P.FPERIOD && ? " +
                                  "      GROUP BY P.FACCOUNT_ID " +
                                  "             ) " +
                                  "       AS P  " +
                                  "       ON M.FACCOUNT_ID = P.FACCOUNT_ID ";


    String TOTAL_USAGE_SELECT = "SELECT " +
                                  "   ROUND(CAST(LEAST("+GBH_SUM+", ?) as numeric), 6) AS FFREE_AMOUNT, " +
                                  "   ROUND(CAST(LEAST(GREATEST("+GBH_SUM+" -?, 0), CASE WHEN P.FAMOUNT IS NULL THEN 0.0 ELSE P.FAMOUNT END) as numeric), 6) AS FPREPAID_AMOUNT, " +
                                  "   ROUND(CAST(GREATEST("+GBH_SUM+" - ? -  CASE WHEN P.FAMOUNT IS NULL THEN 0.0 ELSE P.FAMOUNT END , 0) as numeric), 6) AS FPAID_AMOUNT " +
                                  "FROM " +
                                  "   METRICS  AS M " +
                                  "  LEFT JOIN ( " +
                                  "      SELECT " +
                                  "        " + PREPAID_AMOUNT + " AS FAMOUNT, " +
                                  "        FACCOUNT_ID " +
                                  "      FROM  " +
                                  "        PREPAID AS P" +
                                  "      WHERE  " +
                                  "        P.FPERIOD && ? " +
                                  "      GROUP BY P.FACCOUNT_ID " +
                                  "             ) " +
                                  "       AS P  " +
                                  "       ON M.FACCOUNT_ID = P.FACCOUNT_ID ";


    String CHARGES_MEMORY_INSERT =
            "INSERT INTO " +
            "   CHARGES (" +
            "                   FACCOUNT_ID, " +
            "                   FSERVICE_ID, " +
            "                   FFREE_AMOUNT, " +
            "                   FPREPAID_AMOUNT, " +
            "                   FPAID_AMOUNT, " +
            "                   FPAID_PRICE, " +
            "                   FCALC_ID " +
            "                  ) " +
            "SELECT " +
            "   M.FACCOUNT_ID AS FACCOUNT_ID, " +
            "   ? AS FSERVICE_ID, " +
            "   LEAST(SUM(M.FAMOUNT), ?) AS FFREE_AMOUNT, " +
            "   LEAST(GREATEST(SUM(M.FAMOUNT) -?, 0), CASE WHEN P.FAMOUNT IS NULL THEN 0.0 ELSE P.FAMOUNT END) AS FPREPAID_AMOUNT, " +
            "   GREATEST(SUM(M.FAMOUNT) - ? -  CASE WHEN P.FAMOUNT IS NULL THEN 0.0 ELSE P.FAMOUNT END , 0) AS FPAID_AMOUNT, " +
            "   ? AS FPAID_PRICE, " +
            "   ? as FCALC_ID " +
            "FROM " +
            "  MEMORY_CHARGES AS M " +
            "  LEFT JOIN ( " +
            "      SELECT " +
            "        " + PREPAID_AMOUNT + " AS FAMOUNT, " +
            "        FACCOUNT_ID " +
            "      FROM  " +
            "        PREPAID AS P" +
            "      WHERE  " +
            "        P.FPERIOD && ? " +
            "      GROUP BY P.FACCOUNT_ID " +
            "             ) " +
            "       AS P  " +
            "       ON M.FACCOUNT_ID = P.FACCOUNT_ID " +
            "WHERE " +
            "  M.FCALC_ID = ? " +
            "GROUP BY " +
            "  M.FACCOUNT_ID, " +
            "  P.FAMOUNT ";


    /**
     * Generate invoices from charges.
     */
    String INVOICES_INSERT =
            "INSERT INTO " +
            "   INVOICES(" +
            "                   FTOTAL, " +
            "                   FACCOUNT_ID, " +
            "                   FPAYMENT_STATE, " +
            "                   FCREATED_TIME, " +
            "                   FPERIOD, " +
            "                   FCALC_ID " +
            "                  ) " +
            "SELECT " +
            "   " + TOTAL_SUM + " AS FTOTAL, " +
            "   FACCOUNT_ID AS FACCOUNT_ID, " +
            "  CASE " +
            "   WHEN " + TOTAL_SUM + "> 0.0 THEN '" + PaymentState.WAITING_EXECUTOR.getState() + "'" +
            "   ELSE  '" + PaymentState.NOT_REQUIRED.getState() + "'" +
            "  END as FPAYMENT_STATE, " +
            "   ? as FCREATED_TIME, " +
            "   ? as FPERIOD, " +
            "   ? as FCALC_ID " +
            "FROM " +
            "  CHARGES " +
            "WHERE " +
            "  FCALC_ID = ? " +
            "GROUP BY " +
            "  FACCOUNT_ID ";


    /**
     * Update payment status and credit card of invoices.
     */
    String INVOICES_PAYMENT_STATE_AND_CC_UPDATE = "UPDATE   INVOICES " +
                                                  " SET FPAYMENT_STATE=? , FPAYMENT_TIME=NOW(), FCREDIT_CARD=? " +
                                                  " WHERE FID=? ";
    /**
     * Update payment status of invoices.
     */
    String INVOICES_PAYMENT_STATE_UPDATE        = "UPDATE   INVOICES " +
                                                  " SET FPAYMENT_STATE=? , FPAYMENT_TIME=NOW() " +
                                                  " WHERE FID=? ";
    /**
     * Update mailing time of invoices.
     */
    String INVOICES_MAILING_TIME_UPDATE         = "UPDATE INVOICES " +
                                                  " SET FMAILING_TIME=NOW()" +
                                                  " WHERE FID=? ";

    /**
     * Select charges by given account id and calculation id.
     */
    String CHARGES_SELECT        =
            "SELECT " +
            "                   FSERVICE_ID, " +
            "                   FFREE_AMOUNT, " +
            "                   FPREPAID_AMOUNT, " +
            "                   FPAID_AMOUNT, " +
            "                   FPAID_PRICE " +
            "FROM " +
            "  CHARGES " +
            "WHERE " +
            " FACCOUNT_ID  = ? " +
            " AND FCALC_ID = ? ";
    /**
     * Select memory charges by given account id and calculation id.
     */
    String MEMORY_CHARGES_SELECT =
            "SELECT " +
            "                   FAMOUNT, " +
            "                   FWORKSPACE_ID  " +
            "FROM " +
            "  MEMORY_CHARGES " +
            "WHERE " +
            " FACCOUNT_ID  = ? " +
            " AND FCALC_ID = ? ";


    String METRIC_INSERT = "INSERT INTO METRICS " +
                           "  (" +
                           "      FAMOUNT," +
                           "      FDURING," +
                           "      FUSER_ID," +
                           "      FACCOUNT_ID," +
                           "      FWORKSPACE_ID, " +
                           "      FRUN_ID" +
                           "  )" +
                           "    VALUES (?, ?, ?, ?, ?, ? );";

    String METRIC_SELECT_ID = " SELECT " +
                              "      FAMOUNT," +
                              "      FDURING," +
                              "      FUSER_ID," +
                              "      FACCOUNT_ID," +
                              "      FWORKSPACE_ID,  " +
                              "      FRUN_ID " +
                              "FROM " +
                              "  METRICS " +
                              "WHERE FID=?";

    String METRIC_SELECT_RUNID = " SELECT " +
                                 "      FAMOUNT," +
                                 "      FSTART_TIME," +
                                 "      FSTOP_TIME," +
                                 "      FUSER_ID," +
                                 "      FACCOUNT_ID," +
                                 "      FWORKSPACE_ID,  " +
                                 "      FRUN_ID " +
                                 "FROM " +
                                 "  METRICS " +
                                 "WHERE " +
                                 "  FRUN_ID=? " +
                                 "ORDER BY " +
                                 "  FSTART_TIME";


    String METRIC_SELECT_ACCOUNT_TOTAL = "SELECT " +
                                         "  " + GBH_SUM + " AS FAMOUNT " +
                                         "FROM " +
                                         "  METRICS AS M " +
                                         "WHERE " +
                                         "   M.FACCOUNT_ID=?" +
                                         "   AND M.FDURING && ?";

    String METRIC_SELECT_ACCOUNT_GB_WS_TOTAL = "SELECT " +
                                               "  " + GBH_SUM + " AS FAMOUNT, " +
                                               "   M.FWORKSPACE_ID " +
                                               "FROM " +
                                               "  METRICS AS M " +
                                               "WHERE " +
                                               "   M.FACCOUNT_ID=?" +
                                               "   AND M.FDURING && ?" +
                                               "GROUP BY M.FWORKSPACE_ID";


    String METRIC_UPDATE = "UPDATE  METRICS " +
                           " SET FDURING=? " +
                           " WHERE FID=? ";


    String PREPAID_INSERT = "INSERT INTO PREPAID " +
                            "  (" +
                            "      FACCOUNT_ID," +
                            "      FAMOUNT," +
                            "      FPERIOD," +
                            "      FADDED" +

                            "  )" +
                            "    VALUES (?, ?, ?, now());";


}
