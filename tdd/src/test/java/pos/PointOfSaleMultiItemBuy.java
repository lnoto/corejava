package pos;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;

import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class PointOfSaleMultiItemBuy {

    @Test
    public void zero_items_buy() {

        Display display = new Display();
        MerchantStore store = new MerchantStore(display, null);

        store.onTotal();
        assertEquals("No items selected. Scan again!!!!", display.getText());

    }

    @Test
    public void single_item_buy() {
        Display display = new Display();
        MerchantStore store = new MerchantStore(display, new ProductCatalog(singletonMap("100", "$20"), singletonMap("100", 2000)));

        store.onBarCode("100");
        store.onTotal();

        assertEquals("Total: $20", display.getText());

    }

    @Test
    public void single_item_but_invalid_scan() {
        Display display = new Display();
        MerchantStore store = new MerchantStore(display, new ProductCatalog(Collections.emptyMap(), Collections.emptyMap()));

        store.onBarCode("someinvalid");
        store.onTotal();
        assertEquals("No items selected. Scan again!!!!", display.getText());

    }

    @Test
    public void multiple_items_buy() {
        Display display = new Display();
        MerchantStore store = new MerchantStore(display, new ProductCatalog(new HashMap<String, String>() {{
            put("1", "$5.55");
            put("2", "$6.00");
        }}, new HashMap<String, Integer>() {{
            put("1", 555);
            put("2", 600);
        }}), true);

        store.onBarCode("1");
        store.onBarCode("2");

        store.onTotal();

        assertEquals("Total: $11.55", display.getText());

    }
}
