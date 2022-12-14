package models;

import java.util.Date;

public class Fornecedor {
	
	private String cnpj;
    private String empresa;
    private String telefone;
    private String email;
    private Endereco enderešo;
    private Date criadoEm;

    public Fornecedor() {
        setCriadoEm(new Date());
    }

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }

    public String getEmpresa() {
        return empresa;
    }

    public void setEmpresa(String empresa) {
        this.empresa = empresa;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Endereco getEnderešo() {
        return enderešo;
    }

    public void setEnderešo(Endereco enderešo) {
        this.enderešo = enderešo;
    }

    public Date getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(Date criadoEm) {
        this.criadoEm = criadoEm;
    }
    
}
