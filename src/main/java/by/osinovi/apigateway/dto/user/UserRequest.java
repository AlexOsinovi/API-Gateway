package by.osinovi.apigateway.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserRequest {
    @NotBlank
    @Size(max = 32)
    private String name;
    @NotBlank
    @Size(max = 32)
    private String surname;
    private LocalDate birthDate;
    @NotBlank
    @Email
    @Size(max = 128)
    private String email;
}