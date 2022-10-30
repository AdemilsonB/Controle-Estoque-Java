package views;

import java.util.Scanner;

import controllers.FornecedorController;
import controllers.FuncionarioController;
import models.Funcionario;
import models.Endereco;

public class CadastrarFuncionario {
	
	private static Funcionario funcionario;
	private static Endereco endereco;
	private static Scanner print = new Scanner(System.in);

	 public static void renderizar() {

		 funcionario = new Funcionario();
		 endereco = new Endereco();

		 System.out.println("\n");
		 System.out.println("\n-- CADASTRAR FUNCIONARIO	 --\n");
		 System.out.println("Digite a MATRICULA:");
		 String matricula = print.next();
		 if (FuncionarioController.buscarPorMatricula(matricula) == null) {
			 funcionario.setMatricula(matricula);
			 System.out.println("Digite o nome:");
			 funcionario.setPessoa(print.next());
			 System.out.println("Digite o sobrenome:");
			 funcionario.setSobrenome(print.next());
			 System.out.println("Digite o cpf:");
			 funcionario.setCpf(print.next());
			 
			 System.out.println("Endereco:");
			 System.out.println("Logradouro:");// ver dps
			 endereco.setLogradouro(print.next());
			 System.out.println("Numero:");
			 endereco.setNumero(print.next()); 
			 System.out.println("Bairro:");
			 endereco.setBairro(print.next());
			 System.out.println("Cidade:");
			 endereco.setCidade(print.next());
			 System.out.println("CEP:");
			 endereco.setCep(print.next());
			 System.out.println("Digite a data de admissão:");
			 
			 funcionario.setDataAdmissao(print.next());
			 System.out.println("Setor:");
			 funcionario.setSetor(print.next());			 

			 FuncionarioController.cadastrar(funcionario);
			 System.out.println("\nFuncionario cadastrado com sucesso!");
		 
		 }else {
			 System.out.println("\n Este funcionario já existe!");
		 }
	 }
}


