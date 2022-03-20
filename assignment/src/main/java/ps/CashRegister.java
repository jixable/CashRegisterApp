package ps;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Class to be developed test driven with Mockito.
 *
 * @author Pieter van den Hombergh / Richard van den Ham
 */
class CashRegister {

    private final Clock clock;
    private final Printer printer;
    private final UI ui;
    private final SalesService salesService;

    // Declare a field to keep a salesCache, which is a mapping between a Product and a SalesRecord.
    // When a product gets scanned multiple times, the quantity of the salesRecord is increased. 
    // A LinkedHashMap has the benefit that, in contrast to the HashMap, the order in which 
    // the items were added is preserved.

    // Declare a field to keep track of the last scanned product, initially being null.

    // TODO Declare and initialize fields.
    Product lastScannedProduct = null;
    LinkedHashMap<Product, SalesRecord> salesCache;
    int quantity = 1;


    /**
     * Create a business object
     *
     * @param clock        wall clock
     * @param printer      to use
     * @param ui           to use
     * @param salesService to use
     */
    CashRegister(Clock clock, Printer printer, UI ui, SalesService salesService) {
        this.clock = clock;
        this.printer = printer;
        this.ui = ui;
        this.salesService = salesService;
        //lastScannedProduct = null;
        salesCache = new LinkedHashMap<>();

    }

    /**
     * The scan method is triggered by scanning a product by the cashier.
     * Get the product from the salesService. If the product can't be found, an UnknownProductException is thrown and the
     * error message from the exception is shown on the display (ui).
     * If found, check if there is a salesRecord for this product already. If not, create one. If it exists, update the quantity.
     * In case a perishable product was scanned, the cashier should get a calendar on his/her display.
     * The product is displayed on the display.
     *
     * @param barcode
     */
    public void scan(int barcode) throws UnknownProductException {
        //TODO implement scan

        lastScannedProduct = salesService.lookupProduct(barcode);
        if (lastScannedProduct == null) {
            ui.displayErrorMessage("Unknown Product!");
            throw new UnknownProductException("Unknown Product");
        }
        else if (lastScannedProduct.isPerishable() == false) {
            //salesService.lookupProduct(barcode);
            ui.displayProduct(lastScannedProduct);

        } else {
            ui.displayCalendar();
        }
//        else if ((lastScannedProduct.isPerishable() == true)) {
//            ui.displayCalendar();
            //correctSalesPrice();
//        } else {
//            ui.displayErrorMessage("Unknown Product!");
//            throw new UnknownProductException("Unknown Product");
//        }

        if (salesCache.containsKey(barcode)) { //TODO get the existing Sales Record and change quantity
            SalesRecord existing = null;
            for (SalesRecord sr :
                    salesCache.values()) {
                if (sr.getBarcode() == barcode) {
                    existing = sr;
                }
            }
//            for (Product pr :
//                    salesCache.keySet()) {
//                if (pr.getBarcode() == barcode) {
//                    lastScannedProduct = pr;
//                }
//            }
            existing.increaseQuantity(1);
            quantity++;
            //salesCache.put(lastScannedProduct, existing);
        } else {
//            for (Product pr :
//                    salesCache.keySet()) {
//                if (pr.getBarcode() == barcode) {
//                    lastScannedProduct = pr;
//                }
//            }
            SalesRecord salesRecord = new SalesRecord(barcode, LocalDate.now(), lastScannedProduct.getPrice());
            salesCache.put(lastScannedProduct, salesRecord);
        }



    }

    /**
     * Submit the sales to the sales service, finalizing the sales transaction.
     * All salesRecords in the salesCache are stored (one-by-one) in the salesService.
     * All caches are reset.
     */
    public void finalizeSalesTransaction() {
        //TODO implement finalizeSalesTransaction()
        for (SalesRecord sr :
                salesCache.values()) {
            salesService.sold(sr);
        }
        salesCache.clear();


//        SalesRecord salesRecord = new SalesRecord(lastScannedProduct.getBarcode(), clock , lastScannedProduct.getPrice())
//
//        salesService.sold(salesRecord);

}

    /**
     * Correct the sales price of the last scanned product by considering the
     * given best before date, then submit the product to the service and save
     * in list.
     *
     * This method consults the clock to see if the product is eligible for a
     * price reduction because it is near or at its best before date.
     * 
     * Precondition is that the last scanned product is the perishable product. 
     * You don't need to check that in your code. 
     * 
     * To find the number of days from now till the bestBeforeDate, use
     * LocalDate.now(clock).until(bestBeforeDate).getDays();
     * 
     * Depending on the number of days, update the price in the salesRecord folowing the 
     * pricing strategy as described in the assignment
     *
     * Update the salesRecord belonging to the last scanned product if necessary, so 
     * update the price and set the BestBeforeDate.
     * 
     * @param bestBeforeDate
     * @throws UnknownBestBeforeException in case the best before date is null.
     */
    public void correctSalesPrice(LocalDate bestBeforeDate) throws UnknownBestBeforeException {
        //TODO implement correctSalesPrice
        int percentOff = 0;
        int remainingPricePerc = 100;

//        for (Product pr :
//                salesCache.keySet()) {
//            if (pr.getBarcode() == barcode) {
//                lastScannedProduct = pr;
//            }
//        }
        int brc = lastScannedProduct.getBarcode();

        SalesRecord sc = null;
        for (SalesRecord sr :
                salesCache.values()) {
                if (sr.getBarcode() == brc) {
                    sc = sr;
                }
        }
       if (bestBeforeDate == null) {
           ui.displayErrorMessage("Best before Date is null");
           throw new UnknownBestBeforeException("Unknown Best Before Date");
       } else {
           //Number of days left till expiring
           int left =  LocalDate.now(clock).until(bestBeforeDate).getDays();
           if (left >= 2) {
               percentOff = 0;
               remainingPricePerc = 100;

           } else if (left == 1) {
               percentOff = 30;
               remainingPricePerc = 65;

           } else if ( left == 0) {
               percentOff = 65;
               remainingPricePerc = 35;

           } else if (left < 0) {
               percentOff = 100;
               remainingPricePerc = 0;

           }
           int beforePrice = lastScannedProduct.getPrice();
           int afterPrice = beforePrice/100 * remainingPricePerc;
           sc.setSalesPrice(afterPrice);
           sc.setBestBeforeDate(bestBeforeDate);

       }

        
    }

    /**
     * Print the receipt for all the sold products, to hand the receipt to the
     * customer. The receipt contains lines containing: the product description,
     * the (possibly reduced) sales price per piece and the quantity, separated by
     * a tab.
     * The order of printing is the order of scanning, however Perishable
     * products are printed first. The non-perishables afterwards.
     */
    public void printReceipt() {
        //TODO implement printReceipt
//        Collection prko =new ArrayList();
//        for (Product pr :
//                salesCache.keySet()) {
//            lastScannedProduct = pr;
//            prko.add(lastScannedProduct.getDescription());
//            prko.add(lastScannedProduct.getPrice());
//
//        }
//        for (SalesRecord sr :
//                salesCache.values()) {
//            prko.add(sr.getSalesPrice());
//            prko.add(sr.getQuantity());
//        }
//
//        for (Entry:
//             prko.stream().toArray()) {
//
//        }
        for (Product pr :
                salesCache.keySet()) {
            printer.println(pr.getDescription() + "\t"  + pr.getPrice() + "\t" + quantity);

        }

        
    }
}
