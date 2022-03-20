package ps;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * CashRegister is the business class to test. It gets bar code scanner input,
 * is able to output to the ui, and uses the SalesService.
 *
 * @author Pieter van den Hombergh / Richard van den Ham
 */
@ExtendWith(MockitoExtension.class)
public class CashRegisterTest {

    Product lamp = new Product("led lamp", "Led Lamp", 250, 1_234, false);
    Product banana = new Product("banana", "Bananas Fyffes", 150, 9_234, true);
    Product cheese = new Product("cheese", "Gouda 48+", 800, 7_687, true);
    Clock clock = Clock.systemDefaultZone();

    Map<String, Product> products = Map.of(
            "lamp", lamp,
            "banana", banana,
            "cheese", cheese
    );

    @Mock
    Printer printer;

    @Mock
    SalesService salesService;

    @Mock
    UI ui;

    @Captor
    private ArgumentCaptor<SalesRecord> salesRecordCaptor;

    @Captor
    private ArgumentCaptor<Product> productCaptor;

    @Captor
    private ArgumentCaptor<String> stringLineCaptor;

    CashRegister cashRegister;

    @BeforeEach
    void setup() {
        cashRegister = new CashRegister(clock, printer, ui, salesService);
    }

    /**
     * Test that after a scan, a non perishable product is looked up and
     * correctly displayed.Have a look at requirements in the JavaDoc of the
     * CashRegister methods. Test product is non-perishable, e.g. led lamp.
     * <ul>
     * <li>Train the mocked salesService and check if a lookup has been
     * done.<li>Check if the mocked UI was asked to display the
     * product.<li>Ensure that ui.displayCalendar is not called.<b>NOTE
     *
     * @throws ps.UnknownProductException
     */
    @Test
    public void lookupandDisplayNonPerishableProduct() throws UnknownProductException {

        //TODO 1 Implement Test Method and write necessary implementation in scan() method of CashRegister
        //when()
            salesRecordCaptor = ArgumentCaptor.forClass(SalesRecord.class);
            salesRecordCaptor.capture();
            cashRegister.scan(lamp.getBarcode());
            ui.displayProduct(lamp);
            assertThat(lamp.isPerishable()).isFalse();
            verify(ui, times(0)).displayCalendar();

        //fail( "method lookupandDisplayNonPerishableProduct reached end. You know what to do." );
    }

    /**
     * Test that both the product and calendar are displayed when a perishable
     * product is scanned.
     *
     * @throws UnknownProductException but don't worry about it, since you test
     * with an existing product now.
     */
    @Test
    public void lookupandDisplayPerishableProduct() throws UnknownProductException {
        //TODO 2 Implement Test Method and write necessary implementation in scan() method of CashRegister

        cashRegister.scan(banana.getBarcode());
        ui.displayProduct(banana);
        assertThat(banana.isPerishable()).isTrue();
        verify(ui).displayCalendar();
        //fail( "method lookupandDisplayPerishableProduct reached end. You know what to do." );
    }

    /**
     * Scan a product, finalize the sales transaction, then verify that the
     * correct salesRecord is sent to the SalesService. Use a non-perishable
     * product. SalesRecord has no equals method (and do not add it), instead
     * use {@code assertThat(...).usingRecursiveComparison().isEqualTo(...)}.
     * Also verify that if you print a receipt after finalizing, there is no output.
     *
     * @throws ps.UnknownProductException
     */
    @Test
    public void finalizeSalesTransaction() throws UnknownProductException {
        //TODO 3 Implement Test Method and write necessary implementation in finalizeSalesTransaction() method of CashRegister
        cashRegister.scan(lamp.getBarcode());
        cashRegister.finalizeSalesTransaction();
        SalesRecord expected = new SalesRecord(lamp.getBarcode(), LocalDate.now(), lamp.getPrice());
        stringLineCaptor = ArgumentCaptor.forClass(String.class);
        salesRecordCaptor = ArgumentCaptor.forClass(SalesRecord.class);
        assertThat(salesRecordCaptor.capture()).usingRecursiveComparison().isEqualTo(expected);
        cashRegister.printReceipt();
        verify(printer, times(0)).println(stringLineCaptor.capture());

        //fail( "method finalizeSalesTransaction reached end. You know what to do." );
    }

    /**
     * Verify price reductions. For a perishable product with: 10 days till
     * best-before, no reduction; 2 days till best-before, no reduction; 1 day
     * till best-before, 35% price reduction; 0 days till best-before (so sales
     * date is best-before date), 65% price reduction; -1 days till best-before
     * (product over date), 100% price reduction.
     *
     * Check the correct price using the salesService and an argument captor.
     */
    @ParameterizedTest
    @CsvSource({
        "banana,10,100",
        "banana,2,100",
        "banana,1,65",
        "banana,0,35",
        "banana,-1,0",})
    public void priceReductionNearBestBefore(String productName, int daysBest, int pricePercent) throws UnknownBestBeforeException, UnknownProductException {
        //TODO 4 Implement Test Method and write necessary implementation in correctSalesPrice() method of CashRegister
        LocalDate days = LocalDate.now().plusDays(daysBest);
        productCaptor = ArgumentCaptor.forClass(Product.class);
        cashRegister.correctSalesPrice(days);


        salesRecordCaptor = ArgumentCaptor.forClass(SalesRecord.class);

        //salesService;

        verify(salesRecordCaptor.capture()).setSalesPrice(productCaptor.capture().getPrice()/100*pricePercent);


        //assertThat(cashRegister.correctSalesPrice(daysBest))
        //fail( "method priceReductionNearBestBefore reached end. You know what to do." );
    }

    /**
     * When multiple products are scanned, the resulting lines on the receipt
     * should be perishable first, not perishables last. Scan a banana, led lamp
     * and a cheese. The products should appear on the printed receipt in
     * banana, cheese, lamp order. The printed product line on the receipt
     * should contain description, (reduced) salesprice per piece and the
     * quantity.
     *
     */
    @Test
    public void printInProperOrder() throws UnknownBestBeforeException, UnknownProductException {
        //TODO 5 Implement Test Method and write necessary implementation in printReceipt() method of CashRegister
        cashRegister.scan(lamp.getBarcode());
        cashRegister.scan(banana.getBarcode());
        cashRegister.scan(cheese.getBarcode());

        StringBuilder str = new StringBuilder();
        str.append(lamp.toString());
        str.append(banana.toString());
        str.append(cheese.toString());

        stringLineCaptor = ArgumentCaptor.forClass(String.class);
        List<Product> expected = new ArrayList<>();
        salesRecordCaptor = ArgumentCaptor.forClass(SalesRecord.class);
        List<SalesRecord> actual = salesRecordCaptor.getAllValues();
        cashRegister.printReceipt();
        InOrder inOrder = Mockito.inOrder();


        verify(stringLineCaptor.capture()).equals(str.toString());
       // fail( "method printInProperOrder reached end. You know what to do." );
    }

    /**
     * Test that invoking correctSalesPrice with null parameter results in
     * exception.
     *
     * @throws UnknownProductException (but that one is irrelevant). First scan
     * (scan) a perishable product. Afterwards invoke correctSalesPrice with
     * null parameter. An UnknownProductException should be thrown.
     */
    @Test
    public void correctSalesPriceWithBestBeforeIsNullThrowsException() throws UnknownProductException {
        try {
            Product rhino = new Product("rhino", "Rhino", 250, 1_234, true);
            cashRegister.scan(rhino.getBarcode());
            LocalDate da = null;
            cashRegister.correctSalesPrice(da);
            fail("Should have thrown UnknownBestBeforeException");
        }
        catch (UnknownBestBeforeException e){
                System.out.println(e);
            }
        //TODO 6 Implement Test Method and write necessary implementation in correctSalesPrice() method of CashRegister
        //fail( "method correctSalesPriceWithBestBeforeIsNull reached end. You know what to do." );
    }

    /**
     * Test scanning an unknown product results in error message on GUI.
     */
    @Test
    public void lookupUnknownProductShouldDisplayErrorMessage() throws UnknownProductException {

        //TODO 7 Implement Test Method and write necessary implementation in scan() method of CashRegister
        Product rhino = new Product("rhino", "Rhino", 250, 1_234, false);
        stringLineCaptor = ArgumentCaptor.forClass(String.class);
        cashRegister.scan(rhino.getBarcode());
        //when(cashRegister.scan(rhino.getBarcode())).thenThrow(UnknownProductException.class);
        verify(ui).displayErrorMessage(stringLineCaptor.capture());
        //fail( "method lookupUnknownProduct... reached end. You know what to do." );
    }

    /**
     * Test that a product that is scanned twice, is registered in the
     * salesService with the proper quantity AND make sure printer prints the
     * proper quantity as well.
     *
     * @throws UnknownProductException
     */
    @Test
    public void scanProductTwiceShouldIncreaseQuantity() throws UnknownProductException {
        cashRegister.scan(lamp.getBarcode());
        cashRegister.scan(lamp.getBarcode());
        verify(salesService, times(2)).lookupProduct(lamp.getBarcode());
        //TODO 8 Implement Test Method and write necessary implementation in scan() method of CashRegister
        //fail( "method scanProductTwice reached end. You know what to do." );
    }
}
