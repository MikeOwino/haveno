/*
 * This file is part of Haveno.
 *
 * Haveno is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Haveno is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Haveno. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.desktop.util.validation;

import bisq.core.locale.CurrencyUtil;
import bisq.core.locale.Res;
import bisq.core.payment.validation.InteracETransferAnswerValidator;
import bisq.core.payment.validation.LengthValidator;
import bisq.core.util.validation.RegexValidator;

import bisq.common.config.BaseCurrencyNetwork;
import bisq.common.config.Config;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class InteracETransferAnswerValidatorTest {

    @Before
    public void setup() {
        final BaseCurrencyNetwork baseCurrencyNetwork = Config.baseCurrencyNetwork();
        final String currencyCode = baseCurrencyNetwork.getCurrencyCode();
        Res.setBaseCurrencyCode(currencyCode);
        Res.setBaseCurrencyName(baseCurrencyNetwork.getCurrencyName());
        CurrencyUtil.setBaseCurrencyCode(currencyCode);
    }

    @Test
    public void validate() throws Exception {
        InteracETransferAnswerValidator validator = new InteracETransferAnswerValidator(new LengthValidator(), new RegexValidator());

        assertTrue(validator.validate("abcdefghijklmnopqrstuvwxy").isValid);
        assertTrue(validator.validate("ABCDEFGHIJKLMNOPQRSTUVWXY").isValid);
        assertTrue(validator.validate("1234567890").isValid);
        assertTrue(validator.validate("zZ-").isValid);

        assertFalse(validator.validate(null).isValid); // null
        assertFalse(validator.validate("").isValid); // empty
        assertFalse(validator.validate("two words").isValid); // two words
        assertFalse(validator.validate("ab").isValid); // too short
        assertFalse(validator.validate("abcdefghijklmnopqrstuvwxyz").isValid); // too long
        assertFalse(validator.validate("abc !@#").isValid); // invalid characters
    }

}
