/*
 *  [2012] - [2016] Codenvy, S.A.
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
package com.codenvy.api.creditcard.shared.dto;

import org.eclipse.che.dto.shared.DTO;

/**
 * Describe credit card
 *
 * @author Alexander Garagatyi
 */
@DTO
public interface CreditCard {
    String getToken();

    void setToken(String token);

    CreditCard withToken(String token);

    String getNumber();

    void setNumber(String number);

    CreditCard withNumber(String number);

    String getExpiration();

    void setExpiration(String expiration);

    CreditCard withExpiration(String expiration);

    String getAccountId();

    void setAccountId(String accountId);

    CreditCard withAccountId(String accountId);

    String getCardholder();

    void setCardholder(String cardholder);

    CreditCard withCardholder(String cardholder);

    String getStreetAddress();

    void setStreetAddress(String streetAddress);

    CreditCard withStreetAddress(String streetAddress);

    String getCity();

    void setCity(String city);

    CreditCard withCity(String city);

    String getState();

    void setState(String state);

    CreditCard withState(String state);

    String getCountry();

    void setCountry(String country);

    CreditCard withCountry(String country);

    String getType();

    void setType(String type);

    CreditCard withType(String type);
}
