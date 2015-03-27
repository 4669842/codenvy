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
package com.codenvy.api.account.billing;

import com.codenvy.api.account.impl.shared.dto.CreditCard;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.ServerException;


import java.util.List;

/**
 * Credit card DAO.
 *
 * @author Max Shaposhnik (mshaposhnik@codenvy.com) on 1/26/15.
 */
public interface CreditCardDao {

    String getClientToken(String accountId) throws ServerException, ForbiddenException;

    void registerCard(String accountId, String nonce, String streetAddress, String city, String state, String country)
            throws ServerException, ForbiddenException;

    List<CreditCard> getCards(String accountId) throws ServerException, ForbiddenException;

    void deleteCard(String accountId, String token) throws ServerException, ForbiddenException;
}
