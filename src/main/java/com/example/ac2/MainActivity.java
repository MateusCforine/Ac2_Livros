package com.example.ac2;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    EditText edtTitulo, edtAutor, edtAno;
    Spinner spinnerGenero, spinnerStatus, spinnerFiltroGenero;
    CheckBox checkFavorito, checkFiltroFavoritos;
    Button btnSalvar;
    ListView listViewLivros;
    TextView txtTotal;

    FirebaseFirestore db;

    ArrayList<Livro> listaLivros = new ArrayList<>();
    ArrayList<String> listaExibicao = new ArrayList<>();

    String livroIdEdicao = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        edtTitulo = findViewById(R.id.edtTitulo);
        edtAutor = findViewById(R.id.edtAutor);
        edtAno = findViewById(R.id.edtAno);

        spinnerGenero = findViewById(R.id.spinnerGenero);
        spinnerStatus = findViewById(R.id.spinnerStatus);
        spinnerFiltroGenero = findViewById(R.id.spinnerFiltroGenero);

        checkFavorito = findViewById(R.id.checkFavorito);
        checkFiltroFavoritos = findViewById(R.id.checkFiltroFavoritos);

        btnSalvar = findViewById(R.id.btnSalvar);
        listViewLivros = findViewById(R.id.listViewLivros);
        txtTotal = findViewById(R.id.txtTotal);

        db = FirebaseFirestore.getInstance();

        configurarSpinners();

        btnSalvar.setOnClickListener(v -> salvarOuAtualizarLivro());

        spinnerFiltroGenero.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                carregarLivros();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        checkFiltroFavoritos.setOnCheckedChangeListener((buttonView, isChecked) -> carregarLivros());

        listViewLivros.setOnItemClickListener((parent, view, position, id) -> carregarLivroParaEdicao(position));

        listViewLivros.setOnItemLongClickListener((parent, view, position, id) -> {
            confirmarExclusao(position);
            return true;
        });

        carregarLivros();
    }

    private void configurarSpinners() {
        ArrayAdapter<CharSequence> adapterGenero = ArrayAdapter.createFromResource(
                this,
                R.array.generos_livro,
                android.R.layout.simple_spinner_item
        );
        adapterGenero.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGenero.setAdapter(adapterGenero);

        ArrayAdapter<CharSequence> adapterStatus = ArrayAdapter.createFromResource(
                this,
                R.array.status_livro,
                android.R.layout.simple_spinner_item
        );
        adapterStatus.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(adapterStatus);

        ArrayAdapter<CharSequence> adapterFiltro = ArrayAdapter.createFromResource(
                this,
                R.array.filtro_generos_livro,
                android.R.layout.simple_spinner_item
        );
        adapterFiltro.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFiltroGenero.setAdapter(adapterFiltro);
    }

    private void salvarOuAtualizarLivro() {
        String titulo = edtTitulo.getText().toString().trim();
        String autor = edtAutor.getText().toString().trim();
        String genero = spinnerGenero.getSelectedItem().toString();
        String ano = edtAno.getText().toString().trim();
        String status = spinnerStatus.getSelectedItem().toString();
        boolean favorito = checkFavorito.isChecked();

        if (titulo.isEmpty()) {
            Toast.makeText(this, "O título não pode estar vazio!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (autor.isEmpty()) {
            Toast.makeText(this, "O autor não pode estar vazio!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (genero.equals("Selecione")) {
            Toast.makeText(this, "Selecione um gênero!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ano.isEmpty()) {
            Toast.makeText(this, "O ano não pode estar vazio!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (status.equals("Selecione")) {
            Toast.makeText(this, "Selecione o status de leitura!", Toast.LENGTH_SHORT).show();
            return;
        }

        Livro livro = new Livro(titulo, autor, genero, ano, status, favorito);

        if (livroIdEdicao == null) {
            db.collection("livros")
                    .add(livro)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, "Livro cadastrado com sucesso!", Toast.LENGTH_SHORT).show();
                        limparCampos();
                        carregarLivros();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Erro ao cadastrar livro!", Toast.LENGTH_SHORT).show()
                    );
        } else {
            db.collection("livros")
                    .document(livroIdEdicao)
                    .set(livro)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Livro atualizado com sucesso!", Toast.LENGTH_SHORT).show();
                        limparCampos();
                        carregarLivros();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Erro ao atualizar livro!", Toast.LENGTH_SHORT).show()
                    );
        }
    }

    private void carregarLivros() {
        listaLivros.clear();
        listaExibicao.clear();

        String filtroGenero = spinnerFiltroGenero.getSelectedItem() != null
                ? spinnerFiltroGenero.getSelectedItem().toString()
                : "Todos";

        boolean somenteFavoritos = checkFiltroFavoritos.isChecked();

        db.collection("livros")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    listaLivros.clear();
                    listaExibicao.clear();

                    for (QueryDocumentSnapshot documento : queryDocumentSnapshots) {
                        Livro livro = documento.toObject(Livro.class);
                        livro.setId(documento.getId());

                        boolean passaGenero = filtroGenero.equals("Todos") || livro.getGenero().equals(filtroGenero);
                        boolean passaFavorito = !somenteFavoritos || livro.isFavorito();

                        if (passaGenero && passaFavorito) {
                            listaLivros.add(livro);

                            String item = "Título: " + livro.getTitulo() +
                                    "\nAutor: " + livro.getAutor() +
                                    "\nGênero: " + livro.getGenero() +
                                    "\nAno: " + livro.getAno() +
                                    "\nStatus: " + livro.getStatus() +
                                    "\nFavorito: " + (livro.isFavorito() ? "Sim" : "Não");

                            listaExibicao.add(item);
                        }
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_list_item_1,
                            listaExibicao
                    );

                    listViewLivros.setAdapter(adapter);
                    txtTotal.setText("Total de livros: " + listaLivros.size());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Erro ao carregar livros!", Toast.LENGTH_SHORT).show()
                );
    }

    private void carregarLivroParaEdicao(int position) {
        Livro livro = listaLivros.get(position);

        livroIdEdicao = livro.getId();

        edtTitulo.setText(livro.getTitulo());
        edtAutor.setText(livro.getAutor());
        edtAno.setText(livro.getAno());
        checkFavorito.setChecked(livro.isFavorito());

        ArrayAdapter adapterGenero = (ArrayAdapter) spinnerGenero.getAdapter();
        int posGenero = adapterGenero.getPosition(livro.getGenero());
        spinnerGenero.setSelection(posGenero);

        ArrayAdapter adapterStatus = (ArrayAdapter) spinnerStatus.getAdapter();
        int posStatus = adapterStatus.getPosition(livro.getStatus());
        spinnerStatus.setSelection(posStatus);

        btnSalvar.setText("Atualizar");

        Toast.makeText(this, "Livro carregado para edição!", Toast.LENGTH_SHORT).show();
    }

    private void confirmarExclusao(int position) {
        Livro livro = listaLivros.get(position);

        new AlertDialog.Builder(this)
                .setTitle("Excluir livro")
                .setMessage("Deseja excluir o livro: " + livro.getTitulo() + "?")
                .setPositiveButton("Sim", (dialog, which) -> excluirLivro(livro))
                .setNegativeButton("Não", null)
                .show();
    }

    private void excluirLivro(Livro livro) {
        db.collection("livros")
                .document(livro.getId())
                .delete()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Livro excluído com sucesso!", Toast.LENGTH_SHORT).show();
                    limparCampos();
                    carregarLivros();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Erro ao excluir livro!", Toast.LENGTH_SHORT).show()
                );
    }

    private void limparCampos() {
        edtTitulo.setText("");
        edtAutor.setText("");
        edtAno.setText("");
        spinnerGenero.setSelection(0);
        spinnerStatus.setSelection(0);
        checkFavorito.setChecked(false);
        btnSalvar.setText("Salvar");
        livroIdEdicao = null;
    }
}