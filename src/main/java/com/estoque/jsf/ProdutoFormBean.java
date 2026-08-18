package com.estoque.jsf;

import com.estoque.dto.request.ProdutoRequest;
import com.estoque.service.ProdutoService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

import java.io.Serializable;
import java.math.BigDecimal;

@Named
@ViewScoped
public class ProdutoFormBean implements Serializable {

    private ProdutoService produtoService;

    private String codigo;
    private String nome;
    private Long categoriaId;
    private BigDecimal precoVenda;
    private int estoqueMinimo;

    public ProdutoFormBean() {
    }

    // Construtor usado pelo teste (injeção direta, sem passar pelo Spring).
    ProdutoFormBean(ProdutoService produtoService) {
        this.produtoService = produtoService;
    }

    @PostConstruct
    public void iniciar() {
        if (produtoService == null) {
            jakarta.servlet.ServletContext servletContext =
                    (jakarta.servlet.ServletContext) jakarta.faces.context.FacesContext
                            .getCurrentInstance().getExternalContext().getContext();
            SpringBeanAutowiringSupport.processInjectionBasedOnServletContext(this, servletContext);
        }
    }

    // @Autowired do Spring, não @Inject do CDI — mesma razão documentada em ProdutoListBean.
    @Autowired
    void setProdutoService(ProdutoService produtoService) {
        this.produtoService = produtoService;
    }

    public String criar() {
        // Validação de precoVenda <= 0 é feita pelo Bean Validation em ProdutoRequest via
        // @NotNull @DecimalMin — no fluxo REST isso vira 400 via @Valid no controller; aqui,
        // sem @Valid do JSF, a exceção sobe crua. É intencional: é o "bug" documentado no
        // README (Experimento JSF) para reproduzir o desvio de fase Process Validations →
        // Render Response quando o form JSF ganhar <f:validateBean> numa iteração futura.
        produtoService.criar(new ProdutoRequest(codigo, nome, null, categoriaId, null, null,
                precoVenda, estoqueMinimo));
        return "produtos?faces-redirect=true";
    }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public Long getCategoriaId() { return categoriaId; }
    public void setCategoriaId(Long categoriaId) { this.categoriaId = categoriaId; }
    public BigDecimal getPrecoVenda() { return precoVenda; }
    public void setPrecoVenda(BigDecimal precoVenda) { this.precoVenda = precoVenda; }
    public int getEstoqueMinimo() { return estoqueMinimo; }
    public void setEstoqueMinimo(int estoqueMinimo) { this.estoqueMinimo = estoqueMinimo; }
}
