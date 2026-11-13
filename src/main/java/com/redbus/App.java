
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

        // 6. Extract live bus details
        List<WebElement> busNames = driver.findElements(
            By.xpath("//div[contains(@class,'travelsName')]")
        );

        System.out.println("\n===== LIVE BUS DETAILS =====");

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

                String price = fare.getText()
                    .replace("₹", "")
                    .replace("?", "")
                    .trim();

                int numericPrice = Integer.parseInt(
                    price.replaceAll("[^0-9]", "")
                );

                System.out.println(
                    "Bus Operator: " + busName +
                    " | Ticket Price: ₹" + numericPrice
                );

                busContext.append(busName)
                    .append(" - ₹")
                    .append(numericPrice)
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

        // 7. Demo fallback
        boolean liveResults = count > 0;

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
                    "Demo Bus: " + busName +
                    " | Price: ₹" + numericPrice
                );

                busContext.append(busName)
                    .append(" - ₹")
                    .append(numericPrice)
                    .append(" (DEMO DATA)\n");

                extractedNames.add(busName);
                extractedPrices.add(numericPrice);

                count++;
            }
        }

        if (extractedPrices.isEmpty()) {
            System.out.println("No bus data available.");
            return;
        }

        System.out.println("\nTotal bus results: " + count);

        // 8. Calculate fare insights
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

        // 9. GenAI Travel Assistant
        System.out.println("\n===== ROUTEWISE AI =====");

        String question =
            "Analyze the bus fares and provide a simple, professional comparison. " +
            "Use this exact format:\n\n" +
            "FARE COMPARISON\n" +
            "List bus operators from lowest to highest fare. " +
            "Show rank, operator name and fare in a simple numbered list.\n\n" +
            "CHEAPEST OPTION\n" +
            "Mention the cheapest bus and its fare.\n\n" +
            "PRICE INSIGHT\n" +
            "Mention the difference between the two cheapest options " +
            "and any repeated bus operators with different fares.\n\n" +
            "RECOMMENDATION\n" +
            "Give one short practical recommendation based only on the supplied fares.\n\n" +
            "RULES:\n" +
            "Use plain text only. No Markdown tables, emojis, decorative symbols, " +
            "bold formatting, unnecessary headings or repeated information. " +
            "Keep the response concise and easy to read. " +
            "Do not invent ratings, journey times, amenities or availability.";

        String answer = TravelAssistant.ask(
            busContext.toString(),
            question
        );

        System.out.println("\n===== AI FARE ANALYSIS =====");
        System.out.println(answer);

        // 10. Display terminal insights
        System.out.println("\n===== TRAVEL INSIGHTS =====");

        System.out.println("Total buses: " + count);
        System.out.println("Cheapest bus: " + recommendedName);
        System.out.println("Lowest fare: ₹" + cheapestPrice);
        System.out.println("Highest fare: ₹" + highestPrice);

        System.out.printf("Average fare: ₹%.2f%n", averagePrice);

        // 11. Create RouteWise AI dashboard
        JavascriptExecutor js = (JavascriptExecutor) driver;

        String dataStatus = liveResults
            ? "LIVE RESULTS"
            : "DEMO DATA";

        // Inject draggable dashboard with top-right close button
        js.executeScript("""

            // Remove previous dashboard
            const oldDashboard =
                document.getElementById('routewise-dashboard');

            if (oldDashboard) {
                oldDashboard.remove();
            }

            // Dashboard container
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

            // Header
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

            // Brand
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

            // Close button
            const closeButton = document.createElement('button');

            closeButton.innerHTML = '&times;';
            closeButton.title = 'Close RouteWise AI';
            closeButton.setAttribute(
                'aria-label',
                'Close RouteWise AI'
            );

            closeButton.style.cssText = `
                width:32px;
                height:32px;
                border:none;
                border-radius:8px;
                background:#26374D;
                color:#F8FAFC;
                font-size:25px;
                line-height:1;
                cursor:pointer;
                flex-shrink:0;
            `;

            closeButton.onmouseenter = () => {
                closeButton.style.background = '#DC2626';
            };

            closeButton.onmouseleave = () => {
                closeButton.style.background = '#26374D';
            };

            // Clean up drag listeners when dashboard closes
            const dragController = new AbortController();

            closeButton.onclick = () => {
                dragController.abort();
                dashboard.remove();
            };

            header.appendChild(brand);
            header.appendChild(closeButton);

            dashboard.appendChild(header);

            // Drag functionality
            let dragging = false;
            let offsetX = 0;
            let offsetY = 0;

            header.addEventListener('pointerdown', (event) => {

                if (event.target.closest('button')) {
                    return;
                }

                dragging = true;

                const rect = dashboard.getBoundingClientRect();

                offsetX = event.clientX - rect.left;
                offsetY = event.clientY - rect.top;

                dashboard.style.right = 'auto';
                dashboard.style.bottom = 'auto';

                event.preventDefault();
            }, { signal: dragController.signal });

            document.addEventListener('pointermove', (event) => {

                if (!dragging) return;

                const maxX = window.innerWidth - dashboard.offsetWidth;
                const maxY = window.innerHeight - dashboard.offsetHeight;

                const x = Math.max(
                    0,
                    Math.min(event.clientX - offsetX, maxX)
                );

                const y = Math.max(
                    0,
                    Math.min(event.clientY - offsetY, maxY)
                );

                dashboard.style.left = x + 'px';
                dashboard.style.top = y + 'px';

            }, { signal: dragController.signal });

            document.addEventListener('pointerup', () => {
                dragging = false;
            }, { signal: dragController.signal });

            // Live or demo status
            const status = document.createElement('div');

            status.textContent = arguments[7];

            status.style.cssText = `
                display:inline-block;
                margin-top:15px;
                padding:6px 10px;
                background:#173B3B;
                color:#5EEAD4;
                border-radius:20px;
                font-size:10px;
                font-weight:bold;
            `;

            dashboard.appendChild(status);

            // Route information
            const routeInfo = document.createElement('div');

            routeInfo.style.cssText = `
                margin-top:15px;
                padding:14px;
                background:#1D3048;
                border-radius:10px;
            `;

            routeInfo.innerHTML = `
                <div style="font-size:10px;color:#94A3B8;">
                    YOUR JOURNEY
                </div>

                <div style="font-size:16px;font-weight:bold;margin-top:7px;">
                    ${arguments[0]}
                </div>

                <div style="font-size:12px;color:#94A3B8;margin-top:7px;">
                    ${arguments[1]}
                </div>
            `;

            dashboard.appendChild(routeInfo);

            // Fare metrics
            const metrics = document.createElement('div');

            metrics.style.cssText = `
                display:grid;
                grid-template-columns:repeat(3,1fr);
                gap:8px;
                margin-top:14px;
            `;

            const metricData = [
                ['BUSES', arguments[2]],
                ['LOWEST FARE', '₹' + arguments[3]],
                ['AVERAGE FARE', '₹' + arguments[4]]
            ];

            metricData.forEach(item => {

                const card = document.createElement('div');

                card.style.cssText = `
                    background:#1D3048;
                    padding:12px 7px;
                    border-radius:9px;
                    text-align:center;
                    min-width:0;
                `;

                const label = document.createElement('div');

                label.textContent = item[0];

                label.style.cssText = `
                    font-size:9px;
                    color:#94A3B8;
                    margin-bottom:8px;
                `;

                const value = document.createElement('div');

                value.textContent = item[1];

                value.style.cssText = `
                    font-size:13px;
                    font-weight:bold;
                    color:#5EEAD4;
                    overflow-wrap:anywhere;
                `;

                card.appendChild(label);
                card.appendChild(value);

                metrics.appendChild(card);
            });

            dashboard.appendChild(metrics);

            // Cheapest recommendation
            const recommendation = document.createElement('div');

            recommendation.style.cssText = `
                margin-top:16px;
                padding:15px;
                background:#173B3B;
                border:1px solid #2DD4BF;
                border-radius:10px;
            `;

            const recommendationTitle = document.createElement('div');

            recommendationTitle.textContent = 'CHEAPEST OPTION';

            recommendationTitle.style.cssText = `
                font-size:10px;
                color:#5EEAD4;
                letter-spacing:1px;
                font-weight:bold;
            `;

            const recommendedBus = document.createElement('div');

            recommendedBus.textContent = arguments[5];

            recommendedBus.style.cssText = `
                font-size:16px;
                font-weight:bold;
                margin-top:9px;
            `;

            const recommendedFare = document.createElement('div');

            recommendedFare.textContent = '₹' + arguments[3];

            recommendedFare.style.cssText = `
                font-size:26px;
                font-weight:bold;
                color:#5EEAD4;
                margin-top:6px;
            `;

            recommendation.appendChild(recommendationTitle);
            recommendation.appendChild(recommendedBus);
            recommendation.appendChild(recommendedFare);

            dashboard.appendChild(recommendation);

            // AI analysis heading
            const analysisTitle = document.createElement('div');

            analysisTitle.textContent = 'AI FARE ANALYSIS';

            analysisTitle.style.cssText = `
                margin-top:20px;
                margin-bottom:10px;
                color:#2DD4BF;
                font-size:12px;
                font-weight:bold;
                letter-spacing:1px;
            `;

            dashboard.appendChild(analysisTitle);

            // AI analysis content
            const analysis = document.createElement('div');

            analysis.textContent = arguments[6];

            analysis.style.cssText = `
                background:#1D3048;
                border:1px solid #34445B;
                border-radius:10px;
                padding:14px;
                color:#E2E8F0;
                font-size:13px;
                line-height:1.8;
                white-space:pre-wrap;
                overflow-wrap:anywhere;
                max-height:260px;
                overflow-y:auto;
            `;

            dashboard.appendChild(analysis);

            // Footer
            const footer = document.createElement('div');

            footer.textContent =
                'Make informed travel decisions with RouteWise AI.';

            footer.style.cssText = `
                color:#94A3B8;
                font-size:10px;
                text-align:center;
                margin-top:16px;
            `;

            dashboard.appendChild(footer);

            // Add dashboard to RedBus
            document.body.appendChild(dashboard);

            """,

            "Chennai → Bangalore",
            "21 November · Bus fare intelligence",
            count,
            cheapestPrice,
            String.format("%.0f", averagePrice),
            recommendedName,
            answer,
            dataStatus
        );

        // 12. Highlight cheapest live bus on RedBus
        if (liveResults) {

            List<WebElement> currentBuses = driver.findElements(
                By.xpath("//div[contains(@class,'travelsName')]")
            );

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

                } catch (Exception e) {
                    // Continue searching other bus cards.
                }
            }
        }

        System.out.println("\n===== ROUTEWISE AI COMPLETED =====");
        System.out.println("Dashboard displayed successfully.");
        System.out.println("Drag the header to move the dashboard.");
        System.out.println("Click X to close the dashboard.");
        System.out.println("You can close Chrome manually.");
    }
}
