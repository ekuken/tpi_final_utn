package com.utn.inventario;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
public abstract class ApiTestBase {

    protected static final String ADMIN_EMAIL = "admin@inventario.com";
    protected static final String EMPLEADO_EMAIL = "empleado@inventario.com";

    private static final AtomicInteger SECUENCIA = new AtomicInteger();
    private static String tokenAdminCache;
    private static String tokenEmpleadoCache;

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper json;

    protected String tokenAdmin;
    protected String tokenEmpleado;

    @BeforeEach
    void iniciarSesiones() throws Exception {
        if (tokenAdminCache == null) {
            tokenAdminCache = login(ADMIN_EMAIL, "admin123");
            tokenEmpleadoCache = login(EMPLEADO_EMAIL, "empleado123");
        }
        tokenAdmin = tokenAdminCache;
        tokenEmpleado = tokenEmpleadoCache;
    }

    protected String login(String email, String password) throws Exception {
        ResultActions r = post("/api/auth/login", null, Map.of("email", email, "password", password))
                .andExpect(status().isOk());
        return cuerpo(r).get("token").asText();
    }

    protected ResultActions get(String url, String token) throws Exception {
        return mvc.perform(conToken(MockMvcRequestBuilders.get(url), token));
    }

    protected ResultActions post(String url, String token, Object body) throws Exception {
        return mvc.perform(conToken(conCuerpo(MockMvcRequestBuilders.post(url), body), token));
    }

    protected ResultActions put(String url, String token, Object body) throws Exception {
        return mvc.perform(conToken(conCuerpo(MockMvcRequestBuilders.put(url), body), token));
    }

    protected ResultActions patch(String url, String token, Object body) throws Exception {
        return mvc.perform(conToken(conCuerpo(MockMvcRequestBuilders.patch(url), body), token));
    }

    protected ResultActions delete(String url, String token) throws Exception {
        return mvc.perform(conToken(MockMvcRequestBuilders.delete(url), token));
    }

    protected JsonNode cuerpo(ResultActions r) throws Exception {
        return json.readTree(r.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private MockHttpServletRequestBuilder conCuerpo(MockHttpServletRequestBuilder req, Object body) throws Exception {
        req.contentType(MediaType.APPLICATION_JSON);
        if (body != null) {
            req.content(body instanceof String s ? s : json.writeValueAsString(body));
        }
        return req;
    }

    private MockHttpServletRequestBuilder conToken(MockHttpServletRequestBuilder req, String token) {
        return token == null ? req : req.header("Authorization", "Bearer " + token);
    }

    protected String unico(String prefijo) {
        return prefijo + SECUENCIA.incrementAndGet();
    }

    protected long crearCategoria() throws Exception {
        ResultActions r = post("/api/categorias", tokenAdmin, Map.of("nombre", unico("Cat-")))
                .andExpect(status().isOk());
        return cuerpo(r).get("id").asLong();
    }

    protected long crearProveedor() throws Exception {
        ResultActions r = post("/api/proveedores", tokenAdmin, Map.of("nombre", unico("Proveedor ")))
                .andExpect(status().isOk());
        return cuerpo(r).get("id").asLong();
    }

    protected long crearProducto(Long proveedorId, Long categoriaId, String precio, String costo,
                                 int stock, int stockMinimo) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("codigo", unico("T-"));
        body.put("nombre", unico("Producto "));
        body.put("precio", new BigDecimal(precio));
        body.put("precioCosto", new BigDecimal(costo));
        body.put("stockInicial", stock);
        body.put("stockMinimo", stockMinimo);
        body.put("proveedorId", proveedorId);
        body.put("categoriaId", categoriaId);
        ResultActions r = post("/api/productos", tokenAdmin, body).andExpect(status().isOk());
        return cuerpo(r).get("id").asLong();
    }

    protected long crearProducto(String precio, int stock) throws Exception {
        return crearProducto(null, null, precio, "0", stock, 0);
    }

    protected JsonNode producto(long id) throws Exception {
        return cuerpo(get("/api/productos/" + id, tokenAdmin).andExpect(status().isOk()));
    }

    protected int stock(long productoId) throws Exception {
        return producto(productoId).get("stock").asInt();
    }

    protected static Map<String, Object> item(long productoId, int cantidad) {
        return Map.of("productoId", productoId, "cantidad", cantidad);
    }

    protected static Map<String, Object> itemCompra(long productoId, int cantidad, String costo) {
        return Map.of("productoId", productoId, "cantidad", cantidad, "costoUnitario", new BigDecimal(costo));
    }

    protected static Map<String, Object> venta(List<Map<String, Object>> items) {
        return Map.of("items", items);
    }

    protected JsonNode movimientos(long productoId) throws Exception {
        return cuerpo(get("/api/movimientos/producto/" + productoId, tokenAdmin).andExpect(status().isOk()));
    }

    protected static BigDecimal dinero(JsonNode nodo) {
        return nodo.decimalValue();
    }
}
