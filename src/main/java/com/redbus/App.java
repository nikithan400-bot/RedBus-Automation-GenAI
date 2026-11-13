
package com.redbus;

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

public class App {

    public static void main(String[] args) throws InterruptedException {

        WebDriver driver = new ChromeDriver();

        driver.manage().window().maximize();

        // 1. Open RedBus
        driver.get("https://www.redbus.in/");
        Thread.sleep(3000);

        // 2. Select source
        WebElement from = driver.findElement(By.id("srcinput"));
        from.sendKeys("Chennai");
        Thread.sleep(2000);
        from.sendKeys(Keys.ARROW_DOWN);
        from.sendKeys(Keys.ENTER);

        System.out.println("Chennai selected!");

        // 3. Select destination
        Thread.sleep(1000);

        WebElement to = driver.findElement(By.id("destinput"));
        to.sendKeys("Bangalore");
        Thread.sleep(2000);
        to.sendKeys(Keys.ARROW_DOWN);
        to.sendKeys(Keys.ENTER);

        System.out.println("Bangalore selected!");

        // 4. Select journey date
        Thread.sleep(1000);

        driver.findElement(
            By.xpath("//div[@aria-label='Select date of journey']")
        ).click();

        Thread.sleep(2000);

        WebElement date = driver.findElement(
            By.xpath("//span[@aria-hidden='true' and text()='21']")
        );

        date.click();

        System.out.println("21 November selected!");

        // 5. Search buses
        Thread.sleep(1000);

        driver.findElement(
            By.xpath("//button[@aria-label='Search buses']")
        ).click();

        System.out.println("Bus search completed!");

        Thread.sleep(10000);

        // 6. Extract bus details
        List<WebElement> busNames = driver.findElements(
            By.xpath("//div[contains(@class,'travelsName')]")
        );

        System.out.println("\n===== BUS DETAILS =====");

        int count = 0;
        StringBuilder busContext = new StringBuilder();

        List<String> extractedNames = new ArrayList<>();
        List<Integer> extractedPrices = new ArrayList<>();

        for (WebElement bus : busNames) {

            try {

                WebElement busCard = bus.findElement(
                    By.xpath(
                        "./ancestor::div[.//p[contains(@class,'finalFare')]][1]"
                    )
                );

                String busName = bus.getText().trim();

                WebElement fare = busCard.findElement(
                    By.xpath(".//p[contains(@class,'finalFare')]")
                );

                String price = "\u20B9" +
                    fare.getText().replace("₹", "").replace("?", "").trim();

                int numericPrice = Integer.parseInt(
                    price.replaceAll("[^0-9]", "")
                );

                System.out.println(
                    "Bus Operator: " + busName +
                    " | Ticket Price: " + price
                );

                busContext.append(busName)
                    .append(" - ")
                    .append(price)
                    .append("\n");

                extractedNames.add(busName);
                extractedPrices.add(numericPrice);

                count++;

            } catch (Exception e) {

                System.out.println(
                    "Could not extract details for: " + bus.getText()
                );
            }
        }

        // 7. Demo fallback if RedBus results do not load
        boolean liveResults = count > 0;

        if (count == 0) {

            System.out.println("\nLIVE RESULTS NOT AVAILABLE");
            System.out.println("===== SAMPLE DEMO BUS DATA =====");

            String[][] demoBuses = {
                {"SRI SAI TRAVELS", "798"},
                {"V Bus Holidays", "1079"},
                {"ANT KING", "999"},
                {"SST Limoliner", "1379"},
                {"ACLS Navigator", "1198"}
            };

            for (String[] bus : demoBuses) {

                String busName = bus[0];
                String price = "₹" + bus[1];
                int numericPrice = Integer.parseInt(bus[1]);

                System.out.println(
                    "Bus Operator: " + busName +
                    " | Demo Ticket Price: " + price
                );

                busContext.append(busName)
                    .append(" - ")
                    .append(price)
                    .append(" (DEMO DATA)\n");

                extractedNames.add(busName);
                extractedPrices.add(numericPrice);

                count++;
            }

            System.out.println("Sample data used for demonstration.");
        }

        System.out.println("\nTotal bus details extracted: " + count);

        // 8. AI Travel Assistant
        System.out.println("\n===== AI TRAVEL ASSISTANT =====");

        String answer = TravelAssistant.ask(
            busContext.toString(),
            "Which bus is the cheapest? Compare the available ticket prices."
        );

        System.out.println("AI Recommendation: " + answer);

        // 9. Identify the cheapest bus
        int cheapestIndex = 0;

        for (int i = 1; i < extractedPrices.size(); i++) {

            if (extractedPrices.get(i) <
                extractedPrices.get(cheapestIndex)) {

                cheapestIndex = i;
            }
        }

        String recommendedName = extractedNames.get(cheapestIndex);
        int recommendedPrice = extractedPrices.get(cheapestIndex);

        System.out.println("\n===== CHEAPEST BUS =====");
        System.out.println("Recommended Bus: " + recommendedName);
        System.out.println("Ticket Price: ₹" + recommendedPrice);

        // 10. Display recommendation on RedBus page
        JavascriptExecutor js = (JavascriptExecutor) driver;

        js.executeScript("""
            const oldPanel = document.getElementById('ai-bus-recommendation');

            if (oldPanel) {
                oldPanel.remove();
            }

            const panel = document.createElement('div');
            panel.id = 'ai-bus-recommendation';

            panel.style.cssText =
                'position:fixed;' +
                'top:110px;' +
                'right:20px;' +
                'width:320px;' +
                'padding:20px;' +
                'background:white;' +
                'border:3px solid #16a34a;' +
                'border-radius:14px;' +
                'box-shadow:0 5px 20px rgba(0,0,0,0.25);' +
                'z-index:999999;' +
                'font-family:Arial,sans-serif;' +
                'color:#222;';

            const heading = document.createElement('h3');
            heading.textContent = '✨ AI Recommended Bus';
            heading.style.cssText =
                'color:#15803d;margin:0 0 15px 0;';

            const name = document.createElement('p');
            name.textContent = arguments[0];
            name.style.cssText =
                'font-size:19px;font-weight:bold;margin:8px 0;';

            const price = document.createElement('p');
            price.textContent = 'Ticket Price: ₹' + arguments[1];
            price.style.cssText =
                'font-size:22px;font-weight:bold;color:#15803d;';

            const note = document.createElement('p');
            note.textContent = arguments[2];
            note.style.cssText =
                'font-size:13px;line-height:1.5;margin-top:12px;';

            const close = document.createElement('button');
            close.textContent = 'Close';
            close.style.cssText =
                'padding:8px 18px;border:0;border-radius:6px;' +
                'background:#15803d;color:white;cursor:pointer;';

            close.onclick = () => panel.remove();

            panel.append(heading, name, price, note, close);
            document.body.appendChild(panel);
            """,
            recommendedName,
            recommendedPrice,
            liveResults
                ? "Lowest fare among the live bus results collected. Compare the bus details before booking."
                : "Demo recommendation using sample data. Live bus availability was not loaded."
        );

        // 11. Highlight the recommended live bus
        if (liveResults) {

            List<WebElement> currentBuses = driver.findElements(
                By.xpath("//div[contains(@class,'travelsName')]")
            );

            for (WebElement bus : currentBuses) {

                try {

                    String currentName = bus.getText().trim();

                    if (currentName.equals(recommendedName)) {

                        WebElement card = bus.findElement(
                            By.xpath(
                                "./ancestor::div[.//p[contains(@class,'finalFare')]][1]"
                            )
                        );

                        WebElement fare = card.findElement(
                            By.xpath(".//p[contains(@class,'finalFare')]")
                        );

                        int currentPrice = Integer.parseInt(
                            fare.getText().replaceAll("[^0-9]", "")
                        );

                        if (currentPrice == recommendedPrice) {

                            js.executeScript(
                                "arguments[0].style.outline = '4px solid #16a34a';" +
                                "arguments[0].style.borderRadius = '12px';" +
                                "arguments[0].scrollIntoView({" +
                                "behavior:'smooth',block:'center'});",
                                card
                            );

                            System.out.println(
                                "Recommended bus highlighted on RedBus!"
                            );

                            break;
                        }
                    }

                } catch (Exception e) {
                    // Continue searching other bus cards.
                }
            }
        }

        System.out.println("\n===== AUTOMATION COMPLETED =====");
        System.out.println("You can close Chrome manually when finished.");
    }
}
