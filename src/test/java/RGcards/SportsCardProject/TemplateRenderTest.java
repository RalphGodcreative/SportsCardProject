package RGcards.SportsCardProject;

import RGcards.SportsCardProject.entity.Card;
import RGcards.SportsCardProject.entity.Transaction;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.expression.ThymeleafEvaluationContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.extras.springsecurity6.dialect.SpringSecurityDialect;
import org.thymeleaf.web.servlet.IServletWebExchange;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Renders the card and transaction templates against stub model data.
 *
 * Thymeleaf resolves expressions at request time, so a bad SpEL expression or a
 * mistyped fragment name is invisible until someone loads the page. This walks
 * every card/transaction template — with populated data and again with empty
 * collections, to cover the empty states — and fails on any render error.
 *
 * It deliberately avoids @SpringBootTest: booting the app needs Postgres, and
 * all we want to exercise here is template rendering.
 */
class TemplateRenderTest {

    private SpringTemplateEngine engine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);

        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        // banner.html (included by every page) uses sec:authorize.
        engine.addDialect(new SpringSecurityDialect());
        return engine;
    }

    /**
     * A real servlet-backed context. The pages need one for two reasons: @{/...}
     * link expressions resolve against the context path, and the security
     * dialect refuses to run outside an IWebContext. No authentication is set,
     * so sec:authorize evaluates false and the logged-out nav renders.
     */
    private WebContext baseContext() {
        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        MockHttpServletResponse response = new MockHttpServletResponse();

        JakartaServletWebApplication application =
                JakartaServletWebApplication.buildApplication(servletContext);
        IServletWebExchange exchange = application.buildExchange(request, response);

        WebContext ctx = new WebContext(exchange);
        // SpEL evaluation needs an evaluation context in scope; there's no real
        // application context here, so a static one is enough.
        ctx.setVariable(
                ThymeleafEvaluationContext.THYMELEAF_EVALUATION_CONTEXT_CONTEXT_VARIABLE_NAME,
                new ThymeleafEvaluationContext(new StaticApplicationContext(), null));
        // Spring Security normally puts this in the request; the pages read it
        // for their CSRF meta tags and the banner's logout form.
        ctx.setVariable("_csrf", new StubCsrf());
        return ctx;
    }

    /** Stand-in for Spring Security's CsrfToken. */
    public static class StubCsrf {
        public String getToken() {
            return "test-token";
        }

        public String getHeaderName() {
            return "X-CSRF-TOKEN";
        }

        public String getParameterName() {
            return "_csrf";
        }
    }

    private Card card(int id, String player, Double value, Boolean auto, Long storageId) {
        Card c = new Card();
        c.setId(id);
        c.setYear("2023");
        c.setPublisher("Panini");
        c.setSet("Prizm");
        c.setPlayer(player);
        c.setAuto(auto);
        c.setInsert("Base");
        c.setParallel("Silver");
        c.setNumbered("12/99");
        c.setSports("NBA");
        c.setGrade("PSA 10");
        c.setValue(value);
        c.setNote("a note");
        c.setStorageId(storageId);
        return c;
    }

    private Transaction transaction(int id, String type, Double amount) {
        Transaction t = new Transaction();
        t.setId(id);
        t.setDate(LocalDate.of(2026, 3, 14));
        t.setType(type);
        t.setAmount(amount);
        return t;
    }

    private Map<Long, String> storageNames() {
        Map<Long, String> m = new LinkedHashMap<>();
        m.put(1L, "Binder A");
        m.put(2L, "Safe");
        return m;
    }

    /** Cards including null value / null auto / null storage — the risky rows. */
    private List<Card> cards() {
        List<Card> cards = new ArrayList<>();
        cards.add(card(1, "Victor Wembanyama", 1200.0, true, 1L));
        cards.add(card(2, "Paolo Banchero", null, null, null));
        cards.add(card(3, "C.J. Stroud", 180.5, false, 2L));
        return cards;
    }

    private List<Transaction> transactions() {
        List<Transaction> t = new ArrayList<>();
        t.add(transaction(1, "Buy", 400.0));
        t.add(transaction(2, "Sell", 950.0));
        t.add(transaction(3, "Break", 120.0));
        t.add(transaction(4, "Trade", null));
        t.add(transaction(5, null, 10.0));
        return t;
    }

    private String render(SpringTemplateEngine engine, String template, WebContext ctx) {
        try {
            return engine.process(template, ctx);
        } catch (Exception e) {
            fail("Failed to render " + template + ": " + e.getMessage(), e);
            return null;
        }
    }

    @Test
    void cardTemplatesRender() {
        SpringTemplateEngine engine = engine();

        for (boolean empty : new boolean[]{false, true}) {
            List<Card> cardList = empty ? List.of() : cards();

            WebContext ctx = baseContext();
            ctx.setVariable("cards", cardList);
            ctx.setVariable("cardCounts", cardList.size());
            ctx.setVariable("page", 1);
            ctx.setVariable("storageNames", storageNames());
            ctx.setVariable("storages", List.of());
            ctx.setVariable("lastCard", empty ? null : cards().get(0));

            String all = render(engine, "allCard", ctx);
            String paged = render(engine, "pagingAllCard", ctx);
            String main = render(engine, "cardMain", ctx);
            render(engine, "searchCardPage", ctx);
            render(engine, "addCardPage", ctx);

            if (empty) {
                assertTrue(all.contains("No cards yet"), "allCard should show its empty state");
                assertTrue(main.contains("No cards yet"), "cardMain should show its empty state");
            } else {
                assertTrue(all.contains("Victor Wembanyama"), "allCard should list cards");
                assertTrue(all.contains("badge-auto"), "allCard should badge autos");
                assertTrue(paged.contains("Victor Wembanyama"), "pagingAllCard should list cards");
            }
        }
    }

    @Test
    void transactionTemplatesRender() {
        SpringTemplateEngine engine = engine();

        for (boolean empty : new boolean[]{false, true}) {
            List<Transaction> tranList = empty ? List.of() : transactions();

            WebContext ctx = baseContext();
            ctx.setVariable("transactions", tranList);
            ctx.setVariable("storageNames", storageNames());
            ctx.setVariable("storages", List.of());

            String all = render(engine, "allTransaction", ctx);

            if (empty) {
                assertTrue(all.contains("No transactions yet"), "allTransaction should show its empty state");
            } else {
                // Buy 400 + Break 120 = 520 spent, Sell 950 made.
                assertTrue(all.contains("$520"), "allTransaction should total spend");
                assertTrue(all.contains("$950"), "allTransaction should total sales");
                assertTrue(all.contains("tran-badge-buy"), "allTransaction should badge the type");
            }

            render(engine, "addTransactionPage", ctx);
            render(engine, "sellTransactionPage", ctx);
        }
    }

    @Test
    void transactionDetailRenders() {
        SpringTemplateEngine engine = engine();

        WebContext ctx = baseContext();
        ctx.setVariable("storageNames", storageNames());
        ctx.setVariable("transactionWithCardList", List.of(
                new StubTransactionWithCard(transaction(7, "Sell", 950.0), cards()),
                new StubTransactionWithCard(transaction(8, "Buy", null), List.of())));

        String out = render(engine, "transactionList", ctx);
        assertTrue(out.contains("Transaction #7"), "detail page should title the transaction");
        assertTrue(out.contains("$950.00"), "detail page should format the amount");
        assertTrue(out.contains("No cards on this transaction"),
                "detail page should show the empty state for a transaction with no cards");
    }

    /** Mirrors the shape the controllers put in the model. */
    public static class StubTransactionWithCard {
        private final Transaction transaction;
        private final List<Card> cards;

        StubTransactionWithCard(Transaction transaction, List<Card> cards) {
            this.transaction = transaction;
            this.cards = cards;
        }

        public Transaction getTransaction() {
            return transaction;
        }

        public List<Card> getCards() {
            return cards;
        }
    }
}
