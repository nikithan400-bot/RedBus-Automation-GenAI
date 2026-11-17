
package com.redbus;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class App {

    private static final By BUS_NAMES =
        By.xpath("//div[contains(@class,'travelsName')]");

    private static final By SEARCH_BUTTON =
        By.xpath("//button[@aria-label='Search buses']");

    public static void main(String[] args) throws InterruptedException {

        WebDriver driver = new ChromeDriver();
        driver.manage().window().maximize();

        try {
            // 1. Open RedBus
            driver.get("https://www.redbus.in/");
            Thread.sleep(3000);

            // 2. Collect journey details through chatbot
            Map<String, String> userInput = showChatbotAndWait(driver);

            String fromCity = userInput.get("from");
            String toCity = userInput.get("to");
            String journeyDate = userInput.get("date");
            String preferredTime = userInput.get("time");
            String discountPreference = userInput.get("discount");
            String couponPreference = userInput.get("coupon");

            LocalDate selectedDate = LocalDate.parse(journeyDate);

            String displayDate = selectedDate.format(
                DateTimeFormatter.ofPattern("dd MMMM yyyy")
            );

            System.out.println("\n===== ROUTEWISE USER PREFERENCES =====");
            System.out.println("From: " + fromCity);
            System.out.println("To: " + toCity);
            System.out.println("Date: " + displayDate);
            System.out.println("Departure time: " + preferredTime);
            System.out.println("Discount: " + discountPreference);
            System.out.println("Coupon preference: " + couponPreference);

            // 3. Select source city
            WebDriverWait wait = new WebDriverWait(
                driver, Duration.ofSeconds(20)
            );

            WebElement from = wait.until(
                ExpectedConditions.elementToBeClickable(By.id("srcinput"))
            );

            from.clear();
            from.sendKeys(fromCity);
            Thread.sleep(2000);
            from.sendKeys(Keys.ARROW_DOWN);
            from.sendKeys(Keys.ENTER);

            System.out.println(fromCity + " selected!");

            // 4. Select destination city
            Thread.sleep(1000);

            WebElement to = wait.until(
                ExpectedConditions.elementToBeClickable(By.id("destinput"))
            );

            to.clear();
            to.sendKeys(toCity);
            Thread.sleep(2000);
            to.sendKeys(Keys.ARROW_DOWN);
            to.sendKeys(Keys.ENTER);

            System.out.println(toCity + " selected!");

            // 5. Select journey date
            Thread.sleep(1000);

            wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//div[@aria-label='Select date of journey']")
            )).click();

            Thread.sleep(1500);

            int day = selectedDate.getDayOfMonth();

            WebElement date = wait.until(
                ExpectedConditions.elementToBeClickable(
                    By.xpath(
                        "//span[@aria-hidden='true' and normalize-space()='"
                        + day + "']"
                    )
                )
            );

            date.click();

            System.out.println(displayDate + " selected!");

            // 6. Click actual RedBus Search Buses button
            Thread.sleep(1000);

            String previousUrl = driver.getCurrentUrl();

            wait.until(
                ExpectedConditions.elementToBeClickable(SEARCH_BUTTON)
            ).click();

            System.out.println("RedBus Search Buses clicked!");

            // Wait for results page
            boolean resultsPageOpened;

            try {
                resultsPageOpened = new WebDriverWait(
                    driver, Duration.ofSeconds(30)
                ).until(d ->
                    !d.getCurrentUrl().equals(previousUrl)
                    || !d.findElements(BUS_NAMES).isEmpty()
                );
            } catch (Exception navigationError) {
                resultsPageOpened = false;
            }

            if (!resultsPageOpened) {
                throw new RuntimeException(
                    "RedBus results page did not open. "
                    + "Please check the journey details and try again."
                );
            }

            // Loading appears only after results page opens
            showRecommendationLoading(driver);

            System.out.println(
                "Results page opened. RouteWise AI is analyzing bus details..."
            );

            try {
                new WebDriverWait(driver, Duration.ofSeconds(25))
                    .until(d -> !d.findElements(BUS_NAMES).isEmpty());
            } catch (Exception noResults) {
                System.out.println(
                    "Bus result elements were not detected within the wait time."
                );
            }

            // 7. Extract live bus details
            List<WebElement> busNames = driver.findElements(BUS_NAMES);

            System.out.println("\n===== LIVE BUS DETAILS =====");

            int count = 0;

            StringBuilder busContext = new StringBuilder();

            List<String> extractedNames = new ArrayList<>();
            List<Integer> extractedPrices = new ArrayList<>();
            List<String> extractedCardTexts = new ArrayList<>();

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

                    String priceText = fare.getText()
                        .replace("₹", "")
                        .replace("?", "")
                        .trim();

                    String digits = priceText.replaceAll("[^0-9]", "");

                    if (digits.isEmpty()) {
                        continue;
                    }

                    int numericPrice = Integer.parseInt(digits);

                    String cardText = busCard.getText()
                        .replaceAll("\\s+", " ")
                        .trim();

                    System.out.println(
                        "Bus: " + busName + " | Fare: ₹" + numericPrice
                    );

                    System.out.println("Visible card details: " + cardText);

                    busContext.append("\nBus operator: ")
                        .append(busName)
                        .append("\nDisplayed ticket fare: ₹")
                        .append(numericPrice)
                        .append("\nVisible RedBus card information: ")
                        .append(cardText)
                        .append("\n");

                    extractedNames.add(busName);
                    extractedPrices.add(numericPrice);
                    extractedCardTexts.add(cardText);

                    count++;

                } catch (Exception e) {
                    System.out.println(
                        "Could not extract details for: " + bus.getText()
                    );
                }
            }

            boolean liveResults = count > 0;

            // 8. Demo fallback
            if (count == 0) {
                System.out.println("\nLIVE RESULTS NOT AVAILABLE");
                System.out.println("USING SAMPLE DEMO DATA");

                String[][] demoBuses = {
                    {"SRI SAI TRAVELS", "798"},
                    {"V Bus Holidays", "1079"},
                    {"ANT KING", "999"},
                    {"SST Limoliner", "1379"},
                    {"ACLS Navigator", "1198"}
                };

                for (String[] bus : demoBuses) {
                    String busName = bus[0];
                    int numericPrice = Integer.parseInt(bus[1]);

                    System.out.println(
                        "Demo Bus: " + busName
                        + " | Price: ₹" + numericPrice
                    );

                    busContext.append("\nBus operator: ")
                        .append(busName)
                        .append("\nDisplayed ticket fare: ₹")
                        .append(numericPrice)
                        .append("\n(DEMO DATA - not live RedBus information)\n");

                    extractedNames.add(busName);
                    extractedPrices.add(numericPrice);
                    extractedCardTexts.add("");

                    count++;
                }
            }

            if (extractedPrices.isEmpty()) {
                hideRecommendationLoading(driver);
                System.out.println("No bus data available.");
                return;
            }

            // =====================================================
            // NEW: SPECIAL OFFERS EXTRACTION
            // =====================================================

            System.out.println("\n===== REDBUS SPECIAL OFFERS =====");

            String specialOffers = "";

            try {
                specialOffers = (String) ((JavascriptExecutor) driver)
                    .executeScript("""
                        const bodyText = document.body.innerText || "";

                        const heading =
                            "special offers just for you";

                        const lowerText = bodyText.toLowerCase();

                        const start = lowerText.indexOf(heading);

                        if (start === -1) {
                            return "";
                        }

                        return bodyText.substring(start, start + 3500);
                    """);

            } catch (Exception offerError) {
                System.out.println(
                    "Could not extract special offers: "
                    + offerError.getMessage()
                );
            }

            if (specialOffers != null && !specialOffers.isBlank()) {

                System.out.println(specialOffers);

                busContext.append(
                    "\n\n===== SPECIAL OFFERS JUST FOR YOU =====\n"
                );

                busContext.append(specialOffers);

                busContext.append(
                    "\n===== END OF SPECIAL OFFERS =====\n"
                );

            } else {

                System.out.println(
                    "Special offers section not found in accessible page text."
                );

                busContext.append(
                    "\n\nSPECIAL OFFERS STATUS:\n"
                    + "The Special Offers section was not found "
                    + "in the accessible page text. "
                    + "Do not conclude that no offers exist.\n"
                );
            }

            // =====================================================
            // END OF SPECIAL OFFERS EXTRACTION
            // =====================================================

            // 9. Check discount and coupon information
            boolean discountTextFound = false;
            boolean couponTextFound = false;

            Pattern discountPattern = Pattern.compile(
                "(?i)(discount|%\\s*off|off\\b|save\\s*₹?|saving|offer)"
            );

            Pattern couponPattern = Pattern.compile(
                "(?i)(coupon|promo\\s*code|use\\s*code|code\\s*[:：]|"
                + "RED500|CASH300|IDFC500|RBLCC500|AUBUS200|"
                + "AXIS500|PRIMODAY)"
            );

            for (String cardText : extractedCardTexts) {
                if (cardText == null || cardText.isBlank()) {
                    continue;
                }

                if (discountPattern.matcher(cardText).find()) {
                    discountTextFound = true;
                }

                if (couponPattern.matcher(cardText).find()) {
                    couponTextFound = true;
                }
            }

            // Also check the separate special offers section
            if (specialOffers != null && !specialOffers.isBlank()) {

                if (discountPattern.matcher(specialOffers).find()) {
                    discountTextFound = true;
                }

                if (couponPattern.matcher(specialOffers).find()) {
                    couponTextFound = true;
                }
            }

            String discountStatus = discountTextFound
                ? "Discount or offer-related text was detected in the "
                    + "accessible RedBus information. Inspect the actual "
                    + "offer conditions before stating a discount."
                : "No discount-related text was detected in the accessible "
                    + "bus cards or special offers section. "
                    + "This does not prove that no offers exist.";

            String couponStatus = couponTextFound
                ? "Coupon-related information or known coupon codes "
                    + "were detected in the accessible RedBus information. "
                    + "Inspect the actual codes and eligibility conditions."
                : "No coupon-related text was detected in the accessible "
                    + "bus cards or special offers section. "
                    + "This does not prove that no coupons exist.";

            busContext.append("\nDISCOUNT VISIBILITY CHECK:\n")
                .append(discountStatus)
                .append("\nCOUPON VISIBILITY CHECK:\n")
                .append(couponStatus)
                .append("\n");

            // 10. Calculate fare insights
            int cheapestIndex = 0;
            int highestPrice = 0;
            long totalPrice = 0;

            for (int i = 0; i < extractedPrices.size(); i++) {
                int price = extractedPrices.get(i);

                totalPrice += price;

                if (price < extractedPrices.get(cheapestIndex)) {
                    cheapestIndex = i;
                }

                if (price > highestPrice) {
                    highestPrice = price;
                }
            }

            int cheapestPrice = extractedPrices.get(cheapestIndex);

            double averagePrice =
                (double) totalPrice / extractedPrices.size();

            String recommendedName = extractedNames.get(cheapestIndex);

            // 11. AI analysis request
            String question =
                "The user is planning a journey from " + fromCity
                + " to " + toCity + " on " + displayDate + ".\n"
                + "Preferred departure time: " + preferredTime + ".\n"
                + "Discount preference: " + discountPreference + ".\n"
                + "Coupon preference: " + couponPreference + ".\n\n"

                + "Analyze the supplied bus data and use this format:\n\n"

                + "FARE COMPARISON\n"
                + "List bus operators from lowest to highest fare. "
                + "Show rank, operator name and fare.\n\n"

                + "CHEAPEST OPTION\n"
                + "Mention the cheapest bus and its fare.\n\n"

                + "DEPARTURE TIME MATCH\n"
                + "Compare the user's preferred departure time with "
                + "departure times actually visible in the supplied data. "
                + "If no departure time is visible, say it could not be verified.\n\n"

                + "DISCOUNT DETAILS\n"
                + "Analyze both bus-card offers and the separate "
                + "SPECIAL OFFERS JUST FOR YOU section. "
                + "List visible discounts and their conditions. "
                + "Do not assume that every offer applies to every bus. "
                + "If no discount is visible, say it was not found "
                + "in the accessible information. "
                + "Do not invent discount amounts or percentages.\n\n"

                + "COUPON DETAILS\n"
                + "Analyze the SPECIAL OFFERS section separately. "
                + "Extract every actual coupon code and its accompanying "
                + "description from the supplied text. "
                + "Include first-booking conditions, bank-card conditions, "
                + "operator restrictions and weekday restrictions "
                + "whenever they are shown. "
                + "Do not mistake headings such as FIRST for a coupon code "
                + "unless the supplied text establishes that it is a code. "
                + "If the user's coupon preference is Yes, list the codes. "
                + "If the preference is No, mention that coupon offers "
                + "were not requested. "
                + "Never say no coupon is available when codes are present.\n\n"

                + "PRICE INSIGHT\n"
                + "Mention the difference between the two cheapest options "
                + "when at least two bus fares are available.\n\n"

                + "RECOMMENDATION\n"
                + "Give one practical recommendation based on the fare, "
                + "visible departure time, and verified offers. "
                + "Mention that final coupon applicability and discounted "
                + "fare must be confirmed before payment.\n\n"

                + "STRICT RULES:\n"
                + "Use only the supplied information. Never invent discounts, "
                + "coupon codes, departure times, ratings, amenities or availability. "
                + "Do not treat demo data as live data. "
                + "Do not assume an offer applies to all buses. "
                + "Do not calculate a discounted fare unless eligibility "
                + "and discount are verified. "
                + "Clearly distinguish information that is not visible "
                + "from offers that are confirmed unavailable. "
                + "Use plain text only. Keep the response concise.";

            // 12. Get AI analysis
            String answer;

            try {
                System.out.println("\n===== ROUTEWISE AI =====");

                answer = TravelAssistant.ask(
                    busContext.toString(),
                    question
                );

            } finally {
                hideRecommendationLoading(driver);
            }

            System.out.println("\n===== AI FARE ANALYSIS =====");
            System.out.println(answer);

            // 13. Terminal insights
            System.out.println("\n===== TRAVEL INSIGHTS =====");

            System.out.println("Total buses: " + count);
            System.out.println("Cheapest bus: " + recommendedName);
            System.out.println("Lowest fare: ₹" + cheapestPrice);
            System.out.println("Highest fare: ₹" + highestPrice);
            System.out.printf("Average fare: ₹%.2f%n", averagePrice);

            // 14. Display RouteWise AI dashboard
            JavascriptExecutor js = (JavascriptExecutor) driver;

            String dataStatus = liveResults
                ? "LIVE RESULTS"
                : "DEMO DATA";

            js.executeScript("""
                const oldDashboard =
                    document.getElementById('routewise-dashboard');

                if (oldDashboard) oldDashboard.remove();

                const dashboard = document.createElement('div');
                dashboard.id = 'routewise-dashboard';

                dashboard.style.cssText = `
                    position:fixed;
                    top:20px;
                    right:20px;
                    width:390px;
                    max-width:calc(100vw - 30px);
                    max-height:90vh;
                    overflow-y:auto;
                    background:#101D30;
                    color:#F8FAFC;
                    padding:0 20px 20px;
                    border-radius:16px;
                    border:1px solid #34445B;
                    box-shadow:0 12px 40px rgba(0,0,0,0.45);
                    z-index:999999;
                    font-family:Arial,sans-serif;
                    box-sizing:border-box;
                `;

                const header = document.createElement('div');

                header.style.cssText = `
                    position:sticky;
                    top:0;
                    display:flex;
                    justify-content:space-between;
                    align-items:center;
                    background:#101D30;
                    padding:18px 0 14px;
                    cursor:move;
                    z-index:2;
                    border-bottom:1px solid #34445B;
                    user-select:none;
                    touch-action:none;
                `;

                const brand = document.createElement('div');

                brand.innerHTML = `
                    <div style="font-size:22px;font-weight:bold;color:#2DD4BF;">
                        ◈ RouteWise AI
                    </div>
                    <div style="font-size:9px;letter-spacing:2px;
                        color:#94A3B8;margin-top:4px;">
                        SMART TRAVEL INTELLIGENCE
                    </div>
                `;

                const closeButton = document.createElement('button');
                closeButton.innerHTML = '&times;';
                closeButton.title = 'Close RouteWise AI';

                closeButton.style.cssText = `
                    width:32px;height:32px;border:none;
                    border-radius:8px;background:#26374D;
                    color:#F8FAFC;font-size:25px;cursor:pointer;
                `;

                const dragController = new AbortController();

                closeButton.onclick = () => {
                    dragController.abort();
                    dashboard.remove();
                };

                header.appendChild(brand);
                header.appendChild(closeButton);
                dashboard.appendChild(header);

                let dragging = false;
                let offsetX = 0;
                let offsetY = 0;

                header.addEventListener('pointerdown', event => {
                    if (event.target.closest('button')) return;

                    dragging = true;
                    const rect = dashboard.getBoundingClientRect();

                    offsetX = event.clientX - rect.left;
                    offsetY = event.clientY - rect.top;

                    dashboard.style.right = 'auto';
                    dashboard.style.bottom = 'auto';

                    event.preventDefault();
                }, {signal:dragController.signal});

                document.addEventListener('pointermove', event => {
                    if (!dragging) return;

                    const maxX = window.innerWidth - dashboard.offsetWidth;
                    const maxY = window.innerHeight - dashboard.offsetHeight;

                    dashboard.style.left =
                        Math.max(0, Math.min(event.clientX-offsetX,maxX)) + 'px';

                    dashboard.style.top =
                        Math.max(0, Math.min(event.clientY-offsetY,maxY)) + 'px';
                }, {signal:dragController.signal});

                document.addEventListener('pointerup', () => {
                    dragging = false;
                }, {signal:dragController.signal});

                const status = document.createElement('div');
                status.textContent = arguments[7];

                status.style.cssText = `
                    display:inline-block;margin-top:15px;
                    padding:6px 10px;background:#173B3B;
                    color:#5EEAD4;border-radius:20px;
                    font-size:10px;font-weight:bold;
                `;

                dashboard.appendChild(status);

                const routeInfo = document.createElement('div');

                routeInfo.style.cssText = `
                    margin-top:15px;padding:14px;
                    background:#1D3048;border-radius:10px;
                `;

                const routeTitle = document.createElement('div');
                routeTitle.textContent = 'YOUR JOURNEY';
                routeTitle.style.cssText = 'font-size:10px;color:#94A3B8;';

                const routeValue = document.createElement('div');
                routeValue.textContent = arguments[0];
                routeValue.style.cssText =
                    'font-size:16px;font-weight:bold;margin-top:7px;';

                const routeDate = document.createElement('div');
                routeDate.textContent = arguments[1];
                routeDate.style.cssText =
                    'font-size:12px;color:#94A3B8;margin-top:7px;';

                routeInfo.append(routeTitle,routeValue,routeDate);
                dashboard.appendChild(routeInfo);

                const metrics = document.createElement('div');

                metrics.style.cssText = `
                    display:grid;grid-template-columns:repeat(3,1fr);
                    gap:8px;margin-top:14px;
                `;

                const metricData = [
                    ['BUSES',arguments[2]],
                    ['LOWEST FARE','₹'+arguments[3]],
                    ['AVERAGE FARE','₹'+arguments[4]]
                ];

                metricData.forEach(item => {
                    const card = document.createElement('div');

                    card.style.cssText = `
                        background:#1D3048;padding:12px 7px;
                        border-radius:9px;text-align:center;min-width:0;
                    `;

                    const label = document.createElement('div');
                    label.textContent = item[0];
                    label.style.cssText =
                        'font-size:9px;color:#94A3B8;margin-bottom:8px;';

                    const value = document.createElement('div');
                    value.textContent = item[1];
                    value.style.cssText = `
                        font-size:13px;font-weight:bold;color:#5EEAD4;
                        overflow-wrap:anywhere;
                    `;

                    card.append(label,value);
                    metrics.appendChild(card);
                });

                dashboard.appendChild(metrics);

                const recommendation = document.createElement('div');

                recommendation.style.cssText = `
                    margin-top:16px;padding:15px;background:#173B3B;
                    border:1px solid #2DD4BF;border-radius:10px;
                `;

                const recommendationTitle = document.createElement('div');
                recommendationTitle.textContent = 'CHEAPEST OPTION';
                recommendationTitle.style.cssText = `
                    font-size:10px;color:#5EEAD4;
                    letter-spacing:1px;font-weight:bold;
                `;

                const recommendedBus = document.createElement('div');
                recommendedBus.textContent = arguments[5];
                recommendedBus.style.cssText =
                    'font-size:16px;font-weight:bold;margin-top:9px;';

                const recommendedFare = document.createElement('div');
                recommendedFare.textContent = '₹'+arguments[3];
                recommendedFare.style.cssText = `
                    font-size:26px;font-weight:bold;
                    color:#5EEAD4;margin-top:6px;
                `;

                recommendation.append(
                    recommendationTitle,recommendedBus,recommendedFare
                );

                dashboard.appendChild(recommendation);

                const analysisTitle = document.createElement('div');
                analysisTitle.textContent = 'AI FARE ANALYSIS';
                analysisTitle.style.cssText = `
                    margin-top:20px;margin-bottom:10px;
                    color:#2DD4BF;font-size:12px;
                    font-weight:bold;letter-spacing:1px;
                `;

                dashboard.appendChild(analysisTitle);

                const analysis = document.createElement('div');
                analysis.textContent = arguments[6];

                analysis.style.cssText = `
                    background:#1D3048;border:1px solid #34445B;
                    border-radius:10px;padding:14px;color:#E2E8F0;
                    font-size:13px;line-height:1.8;
                    white-space:pre-wrap;overflow-wrap:anywhere;
                    max-height:260px;overflow-y:auto;
                `;

                dashboard.appendChild(analysis);

                const footer = document.createElement('div');
                footer.textContent =
                    'Make informed travel decisions with RouteWise AI.';

                footer.style.cssText = `
                    color:#94A3B8;font-size:10px;
                    text-align:center;margin-top:16px;
                `;

                dashboard.appendChild(footer);
                document.body.appendChild(dashboard);
                """,
                fromCity + " → " + toCity,
                displayDate + " · Bus fare intelligence",
                count,
                cheapestPrice,
                String.format("%.0f", averagePrice),
                recommendedName,
                answer,
                dataStatus
            );

            // 15. Highlight cheapest live bus
            if (liveResults) {
                List<WebElement> currentBuses =
                    driver.findElements(BUS_NAMES);

                for (WebElement bus : currentBuses) {
                    try {
                        if (bus.getText().trim().equals(recommendedName)) {
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

                            if (currentPrice == cheapestPrice) {
                                js.executeScript("""
                                    arguments[0].style.outline =
                                        '4px solid #0D9488';

                                    arguments[0].style.borderRadius = '12px';

                                    arguments[0].scrollIntoView({
                                        behavior:'smooth',
                                        block:'center'
                                    });
                                    """, card);

                                System.out.println(
                                    "Recommended bus highlighted on RedBus!"
                                );

                                break;
                            }
                        }
                    } catch (Exception ignored) {
                        // Continue checking remaining bus cards.
                    }
                }
            }

            System.out.println("\n===== ROUTEWISE AI COMPLETED =====");
            System.out.println("Dashboard displayed successfully.");
            System.out.println("Drag the header to move the dashboard.");
            System.out.println("Click X to close the dashboard.");
            System.out.println("You can close Chrome manually.");

        } catch (Exception e) {
            try {
                hideRecommendationLoading(driver);
            } catch (Exception ignored) {
                // Browser may already be closed.
            }

            e.printStackTrace();
        }
    }

    // 16. Conversational RouteWise AI chatbot
    public static Map<String, String> showChatbotAndWait(
        WebDriver driver
    ) throws InterruptedException {

        JavascriptExecutor js = (JavascriptExecutor) driver;

        js.executeScript("""
            const old = document.getElementById('routewise-input');
            if (old) old.remove();

            window.routeWiseRequest = null;

            const panel = document.createElement('div');
            panel.id = 'routewise-input';

            panel.style.cssText = `
                position:fixed;right:20px;bottom:20px;
                width:360px;max-width:calc(100vw - 30px);
                height:520px;max-height:80vh;
                display:flex;flex-direction:column;
                background:#101D30;color:white;
                border:1px solid #34445B;border-radius:16px;
                box-shadow:0 10px 35px rgba(0,0,0,.4);
                z-index:999999;font-family:Arial,sans-serif;
                box-sizing:border-box;overflow:hidden;
            `;

            panel.innerHTML = `
                <div style="padding:16px;background:#101D30;
                    border-bottom:1px solid #34445B;">
                    <div style="font-size:21px;font-weight:bold;color:#2DD4BF;">
                        ◈ RouteWise AI
                    </div>
                    <div style="font-size:11px;color:#94A3B8;margin-top:4px;">
                        Your Smart Travel Assistant
                    </div>
                </div>

                <div id="rw-chat-messages" style="
                    flex:1;overflow-y:auto;padding:14px;
                    display:flex;flex-direction:column;gap:12px;
                "></div>

                <div style="padding:12px;border-top:1px solid #34445B;">
                    <div id="rw-input-area"></div>
                    <div id="rw-status" style="
                        color:#5EEAD4;font-size:11px;margin-top:8px;
                    "></div>
                </div>
            `;

            const style = document.createElement('style');

            style.textContent = `
                #routewise-input input,
                #routewise-input select {
                    width:100%;padding:11px;margin-top:7px;
                    border:1px solid #475569;border-radius:8px;
                    background:#1D3048;color:white;
                    box-sizing:border-box;font-size:13px;
                }

                #routewise-input button {
                    width:100%;padding:11px;margin-top:9px;
                    border:0;border-radius:8px;
                    background:#0D9488;color:white;
                    font-weight:bold;cursor:pointer;
                }

                #routewise-input button:hover {
                    background:#0F766E;
                }

                .rw-bot {
                    align-self:flex-start;background:#1D3048;
                    padding:11px 13px;border-radius:12px 12px 12px 3px;
                    max-width:90%;font-size:13px;line-height:1.5;
                    overflow-wrap:anywhere;
                }

                .rw-user {
                    align-self:flex-end;background:#0D766E;
                    padding:10px 13px;border-radius:12px 12px 3px 12px;
                    max-width:85%;font-size:13px;line-height:1.5;
                    overflow-wrap:anywhere;
                }

                #rw-input-area label {
                    display:block;color:#CBD5E1;font-size:11px;
                    margin-bottom:3px;
                }
            `;

            document.head.appendChild(style);
            document.body.appendChild(panel);

            const messages =
                document.getElementById('rw-chat-messages');

            const inputArea =
                document.getElementById('rw-input-area');

            const status =
                document.getElementById('rw-status');

            const answers = {};
            let step = 0;

            const questions = [
                {
                    key:'from',
                    prompt:'Hello! Welcome to RouteWise AI. Where are you travelling from?',
                    placeholder:'Enter your departure city',
                    type:'text'
                },
                {
                    key:'to',
                    prompt:'Great! Where would you like to go?',
                    placeholder:'Enter your destination city',
                    type:'text'
                },
                {
                    key:'date',
                    prompt:'What is your journey date?',
                    placeholder:'',
                    type:'date'
                },
                {
                    key:'time',
                    prompt:'What is your preferred departure time? You can type an exact time or a description, such as 9:30 AM, 11 AM, Morning 11, Around 6 PM, or Any time.',
                    placeholder:'Example: Morning 11',
                    type:'text'
                },
                {
                    key:'discount',
                    prompt:'What discount would you prefer? RouteWise AI will check any available discounts.',
                    placeholder:'Any available discount',
                    type:'text',
                    defaultValue:'Any available discount'
                },
                {
                    key:'coupon',
                    prompt:'Would you like to see available coupon offers?',
                    placeholder:'',
                    type:'select'
                }
            ];

            function addMessage(text, type) {
                const bubble = document.createElement('div');
                bubble.className = type === 'user' ? 'rw-user' : 'rw-bot';
                bubble.textContent = text;
                messages.appendChild(bubble);
                messages.scrollTop = messages.scrollHeight;
            }

            function renderQuestion() {
                const q = questions[step];

                addMessage(q.prompt, 'bot');
                inputArea.innerHTML = '';

                const label = document.createElement('label');
                label.textContent =
                    q.key === 'coupon' ? 'Choose Yes or No' : 'Your answer';

                inputArea.appendChild(label);

                let field;

                if (q.type === 'select') {
                    field = document.createElement('select');

                    const yes = document.createElement('option');
                    yes.value = 'Yes';
                    yes.textContent = 'Yes';

                    const no = document.createElement('option');
                    no.value = 'No';
                    no.textContent = 'No';

                    field.append(yes,no);
                } else {
                    field = document.createElement('input');
                    field.type = q.type;
                    field.placeholder = q.placeholder || '';

                    if (q.defaultValue) {
                        field.value = q.defaultValue;
                    }

                    if (q.type === 'date') {
                        const today = new Date();
                        const localToday = new Date(
                            today.getTime() - today.getTimezoneOffset()*60000
                        ).toISOString().slice(0,10);

                        field.min = localToday;
                    }
                }

                field.id = 'rw-current-answer';
                inputArea.appendChild(field);

                const button = document.createElement('button');
                button.textContent =
                    step === questions.length - 1 ? 'Finish' : 'Send';

                button.onclick = submitAnswer;
                inputArea.appendChild(button);

                field.addEventListener('keydown', event => {
                    if (event.key === 'Enter' && q.type !== 'select') {
                        event.preventDefault();
                        submitAnswer();
                    }
                });

                field.focus();
            }

            function submitAnswer() {
                const q = questions[step];
                const field = document.getElementById('rw-current-answer');

                const value = field.value.trim();

                if (!value) {
                    status.textContent = 'Please enter your answer.';
                    field.focus();
                    return;
                }

                if (q.key === 'date') {
                    const today = new Date();
                    const localToday = new Date(
                        today.getTime() - today.getTimezoneOffset()*60000
                    ).toISOString().slice(0,10);

                    if (value < localToday) {
                        status.textContent =
                            'Please select today or a future date.';
                        return;
                    }
                }

                if (q.key === 'to' &&
                    value.toLowerCase() === answers.from.toLowerCase()) {
                    status.textContent =
                        'Source and destination must be different.';
                    return;
                }

                answers[q.key] = value;

                addMessage(value, 'user');
                status.textContent = '';
                step++;

                if (step < questions.length) {
                    renderQuestion();
                    return;
                }

                window.routeWiseRequest = {
                    from:answers.from,
                    to:answers.to,
                    date:answers.date,
                    time:answers.time,
                    discount:answers.discount,
                    coupon:answers.coupon
                };

                addMessage(
                    'Thank you! Your journey details are collected. '
                    + 'I am sending them to RedBus now.',
                    'bot'
                );

                inputArea.innerHTML = `
                    <div style="font-size:12px;color:#5EEAD4;
                        text-align:center;padding:8px;">
                        Journey details submitted successfully.
                    </div>
                `;

                status.textContent = 'Starting RedBus search...';
            }

            renderQuestion();
            """);

        System.out.println(
            "RouteWise AI chatbot opened. Waiting for user answers..."
        );

        long timeout = System.currentTimeMillis() + 600000;

        while (System.currentTimeMillis() < timeout) {
            Object result = js.executeScript(
                "return window.routeWiseRequest || null;"
            );

            if (result instanceof Map<?, ?>) {
                Map<?, ?> data = (Map<?, ?>) result;

                Map<String, String> input = new HashMap<>();

                input.put("from", String.valueOf(data.get("from")));
                input.put("to", String.valueOf(data.get("to")));
                input.put("date", String.valueOf(data.get("date")));
                input.put("time", String.valueOf(data.get("time")));
                input.put("discount", String.valueOf(data.get("discount")));
                input.put("coupon", String.valueOf(data.get("coupon")));

                js.executeScript("""
                    const panel = document.getElementById('routewise-input');
                    if (panel) panel.remove();
                    """);

                return input;
            }

            Thread.sleep(500);
        }

        throw new RuntimeException(
            "Chatbot input timeout. Please run the project again."
        );
    }

    // 17. Show loading after RedBus results page opens
    public static void showRecommendationLoading(WebDriver driver) {

        JavascriptExecutor js = (JavascriptExecutor) driver;

        js.executeScript("""
            const old = document.getElementById('rw-ai-loading');
            if (old) old.remove();

            const loading = document.createElement('div');
            loading.id = 'rw-ai-loading';

            loading.style.cssText = `
                position:fixed;right:25px;bottom:25px;
                width:300px;padding:20px;
                background:#101D30;color:white;
                border:1px solid #2DD4BF;border-radius:14px;
                box-shadow:0 10px 35px rgba(0,0,0,0.4);
                z-index:1000000;font-family:Arial,sans-serif;
                box-sizing:border-box;
            `;

            loading.innerHTML = `
                <div style="font-size:18px;font-weight:bold;color:#2DD4BF;">
                    ◈ RouteWise AI
                </div>

                <div style="margin-top:18px;font-size:15px;font-weight:bold;">
                    Please wait!
                </div>

                <div style="font-size:12px;color:#CBD5E1;
                    margin-top:10px;line-height:1.6;">
                    RedBus results opened. Analysing bus fares,
                    departure times and available offers...
                </div>

                <div style="height:5px;background:#34445B;
                    border-radius:10px;margin-top:17px;overflow:hidden;">
                    <div style="height:100%;width:40%;background:#2DD4BF;
                        border-radius:10px;
                        animation:rwProgress 1.2s ease-in-out infinite alternate;">
                    </div>
                </div>

                <style>
                    @keyframes rwProgress {
                        from { width:15%; }
                        to { width:95%; }
                    }
                </style>
            `;

            document.body.appendChild(loading);
            """);
    }

    // 18. Hide loading after AI analysis
    public static void hideRecommendationLoading(WebDriver driver) {

        JavascriptExecutor js = (JavascriptExecutor) driver;

        js.executeScript("""
            const loading = document.getElementById('rw-ai-loading');
            if (loading) loading.remove();
            """);
    }
}
